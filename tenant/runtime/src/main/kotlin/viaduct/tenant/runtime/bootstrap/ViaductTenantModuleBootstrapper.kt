package viaduct.tenant.runtime.bootstrap

import graphql.schema.FieldCoordinates
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions
import viaduct.api.Resolver
import viaduct.api.TenantCodeInjector
import viaduct.api.Variables
import viaduct.api.context.ExecutionContext
import viaduct.api.context.FieldExecutionContext
import viaduct.api.context.NodeExecutionContext
import viaduct.api.internal.NodeResolverBase
import viaduct.api.internal.NodeResolverFor
import viaduct.api.internal.ResolverBase
import viaduct.api.internal.ResolverFor
import viaduct.api.types.Arguments
import viaduct.api.types.NodeObject
import viaduct.api.types.Query
import viaduct.engine.api.FieldResolverExecutor
import viaduct.engine.api.NodeResolverExecutor
import viaduct.engine.api.TenantModuleBootstrapper
import viaduct.engine.api.TenantModuleException
import viaduct.engine.api.ViaductSchema
import viaduct.tenant.runtime.context.factory.ArgumentsFactory
import viaduct.tenant.runtime.context.factory.FieldExecutionContextMetaFactory
import viaduct.tenant.runtime.context.factory.MutationFieldExecutionContextMetaFactory
import viaduct.tenant.runtime.context.factory.NodeExecutionContextMetaFactory
import viaduct.tenant.runtime.context.factory.NodeResolverContextFactory
import viaduct.tenant.runtime.context.factory.ObjectFactory
import viaduct.tenant.runtime.context.factory.ResolverContextFactory
import viaduct.tenant.runtime.context.factory.SelectionSetFactory as SelectionSetContextFactory
import viaduct.tenant.runtime.execution.FieldBatchResolverExecutorImpl
import viaduct.tenant.runtime.execution.FieldUnbatchedResolverExecutorImpl
import viaduct.tenant.runtime.execution.NodeBatchResolverExecutorImpl
import viaduct.tenant.runtime.execution.NodeUnbatchedResolverExecutorImpl
import viaduct.tenant.runtime.globalid.GlobalIDCodecImpl
import viaduct.tenant.runtime.internal.ReflectionLoaderImpl
import viaduct.utils.slf4j.logger
import viaduct.utils.string.capitalize

/**
 * ViaductTenantModuleBootstrapper is responsible for bootstrapping the Viaduct tenant module.
 * It is responsible for discovering all the resolvers in the tenant module and creating the resolver executors.
 * We use the package name of the tenant module as a prefix to find all resolvers (including the codegen
 * base resolvers).
 *
 * @param injector Injector to be used in all resolvers for [tenantModule]].
 * @param tenantModulePackage Viaduct tenant module package name.
 */
class ViaductTenantModuleBootstrapper(
    private val tenantCodeInjector: TenantCodeInjector,
    private val tenantResolverClassFinder: TenantResolverClassFinder
) : TenantModuleBootstrapper {
    private val reflectionLoader = ReflectionLoaderImpl { name -> tenantResolverClassFinder.grtClassForName(name) }
    private val globalIDCodec = GlobalIDCodecImpl(reflectionLoader)
    private val requiredSelectionSetFactory = RequiredSelectionSetFactory(globalIDCodec, reflectionLoader)

    /**
     * Return a list of Pair<field-coordinate, resolver executor>s for this Viaduct tenant module.
     * Each time the get is called, we recompute the resolver executor map.
     * Please call this only once or refactor this API to init the map only once.
     */
    override fun fieldResolverExecutors(schema: ViaductSchema): Iterable<Pair<Pair<String, String>, FieldResolverExecutor>> {
        val result: MutableMap<Pair<String, String>, FieldResolverExecutor> = mutableMapOf()
        val tenantFunctionClass = ResolverBase::class.java

        // Get all classes annotated with @ResolverFor in TenantModule's package
        val resolverForClasses: Set<Class<*>> = tenantResolverClassFinder.resolverClassesInPackage()
        val resolverBaseClasses: List<Class<out ResolverBase<*>>> =
            resolverForClasses.map {
                if (!tenantFunctionClass.isAssignableFrom(it)) {
                    throw TenantModuleException("Found @ResolverFor on non TenantFunction ($it)")
                }
                it.asSubclass(tenantFunctionClass)
            }
        @Suppress("UNCHECKED_CAST")
        val resolverClassesByBaseClass: Map<Class<out ResolverBase<*>>, List<Class<out ResolverBase<*>>>> =
            resolverBaseClasses.associate {
                // Get all @Resolver subclasses
                it to tenantResolverClassFinder.getSubTypesOf(it.name).filter { it.kotlin.hasAnnotation<Resolver>() } as List<Class<out ResolverBase<*>>>
            }
        for ((baseClass, resolverClasses) in resolverClassesByBaseClass) {
            val resolverForAnnotation = baseClass.annotations.firstOrNull { it is ResolverFor } as? ResolverFor
                ?: throw TenantModuleException("ResolverBase class $baseClass does not have a @ResolverFor annotation")
            val typeName = resolverForAnnotation.typeName
            val fieldName = resolverForAnnotation.fieldName
            if (resolverClasses.size != 1) {
                throw TenantModuleException("Expected exactly one resolver implementation for $typeName.$fieldName, found ${resolverClasses.size}: ${resolverClasses.map { it.name }}")
            }
            val resolverClass = resolverClasses.first()

            // We register providers for the resolvers here, and this is the only place where we discover Resolver classes
            // for Viaduct, register their providers with the injector provided in the constructor for this class,
            // and create the resolver executors for Viaduct engine.
            val resolverContainerProvider = try {
                tenantCodeInjector.getProvider(resolverClass)
            } catch (e: NoClassDefFoundError) {
                // This can happen, at times, for tenant JARs whose dependencies don't resolve.
                // By re-throwing a TenantModuleException, we ensure we only skip the bootstrapping of one offending tenant.
                throw TenantModuleException("Resolver class $resolverClass could not be injected into", e)
            }
            val resolverKClass = resolverClass.kotlin
            val resolverAnnotation = resolverKClass.annotations.firstOrNull { it is Resolver } as? Resolver
                ?: throw TenantModuleException("Resolver class $resolverKClass does not have a @Resolver annotation")

            // validate that the Resolver defines a maximum of one @Variables-annotated class
            resolverClass.declaredClasses
                .filterNot { it.isSynthetic }
                .filter { it.kotlin.hasAnnotation<Variables>() }
                .let {
                    check(it.size <= 1) {
                        "Resolver class $resolverKClass cannot have more than one nested class with @Variables"
                    }
                }

            @Suppress("UNCHECKED_CAST")
            val contextKClass =
                baseClass.declaredClasses.firstOrNull { ExecutionContext::class.java.isAssignableFrom(it) }?.kotlin as? KClass<FieldExecutionContext<*, *, *, *>>
                    ?: throw java.lang.IllegalArgumentException("No nested Context class found in ${baseClass.name}")
            val noArgs = contextKClass.supertypes.firstOrNull { it.classifier == FieldExecutionContext::class }
                ?.let { it.arguments.any { kType -> kType.type?.classifier == Arguments.NoArguments::class } }
                ?: false
            val argumentsFactory =
                if (noArgs) {
                    ArgumentsFactory.NoArguments
                } else {
                    ArgumentsFactory.forClass(findArgumentsClass(tenantResolverClassFinder, typeName, fieldName))
                }

            val objectKClass = tenantResolverClassFinder.grtClassForName(typeName)
            val queryKClass = tenantResolverClassFinder.grtClassForName(schema.schema.queryType.name) as? KClass<Query>
                ?: throw IllegalArgumentException("GRT for Query type ${schema.schema.queryType.name} is not of type `Query`")
            val resolverId = typeName to fieldName
            val formattedResolverId = formatResolverId(resolverId)

            // While the schema is used here on the next two lines, it
            // is not _captured_ here.  That is, the `forField`
            // function called below basically uses the name to the
            // field's type to return something that does not contain
            // a pointer to any piece of the schema.
            //
            // What it returns _does_ have a pointer to the GRT class
            // for that type, i.e., it captures some version-scoped
            // code state.  However, this is probably okay because the
            // resolvers for this module will get rebuilt when new
            // code comes in.
            val fieldDef = schema.schema.getFieldDefinition(FieldCoordinates.coordinates(typeName, fieldName))
            if (fieldDef == null) {
                log.warn("Found resolver code for $typeName.$fieldName, which is unknown in the schema")
                continue
            }
            val selectionSetContextFactory = SelectionSetContextFactory.forField(fieldDef)
            val (objectSelectionSet, querySelectionSet) = requiredSelectionSetFactory.mkRequiredSelectionSets(
                schema,
                tenantCodeInjector,
                resolverKClass,
                argumentsFactory,
                resolverAnnotation,
                typeName,
            )
            val resolverContextFactory = ResolverContextFactory.forClass(
                contextKClass,
                MutationFieldExecutionContextMetaFactory.ifMutation(
                    contextKClass,
                    FieldExecutionContextMetaFactory.create(
                        objectValue = ObjectFactory.forClass(objectKClass),
                        queryValue = ObjectFactory.forClass(queryKClass),
                        argumentsFactory,
                        selectionSetContextFactory
                    )
                )
            )

            // Java classes do not have the `resolve` function since it is suspended,
            // The implementation should be using a proxy class that hides that complexity
            // and implements the `resolve` function.
            // So for java classes, we need to use the `resolve` function from the base class
            val (resolveFunction, batchResolveFunction) = if (resolverClass.isKotlinClass) {
                resolverKClass.declaredMemberFunctions.firstOrNull { it.name == "resolve" } to
                    resolverKClass.declaredMemberFunctions.firstOrNull { it.name == "batchResolve" }
            } else {
                resolverKClass.memberFunctions.firstOrNull { it.name == "resolve" } to
                    resolverKClass.memberFunctions.firstOrNull { it.name == "batchResolve" }
            }

            if (resolveFunction == null && batchResolveFunction == null) {
                throw TenantModuleException("Resolver class $resolverKClass does not have a 'resolve' nor a 'batchResolve' function")
            }
            if (resolveFunction != null && batchResolveFunction != null) {
                throw TenantModuleException("Resolver class $resolverKClass implements both 'resolve' and 'batchResolve', it should only implement one")
            }
            if (resolveFunction != null) {
                log.info(
                    "- Adding entry for resolver for '$typeName.$fieldName' " +
                        "to '${resolverKClass.qualifiedName}' via ${resolverClass.classLoader}"
                )
                val resolverExecutor = FieldUnbatchedResolverExecutorImpl(
                    objectSelectionSet = objectSelectionSet,
                    querySelectionSet = querySelectionSet,
                    resolver = resolverContainerProvider,
                    resolveFn = resolveFunction,
                    resolverId = formattedResolverId,
                    globalIDCodec = globalIDCodec,
                    reflectionLoader = reflectionLoader,
                    resolverContextFactory = resolverContextFactory,
                )
                result.put(resolverId, resolverExecutor)?.let { extant ->
                    throw RuntimeException(
                        "Duplicate resolver for type $typeName and field $fieldName. " +
                            "Found $extant in class '${resolverKClass.qualifiedName}'."
                    )
                }
            } else if (batchResolveFunction != null) {
                log.info(
                    "- Adding entry for batchResolver for '$typeName.$fieldName' " +
                        "to '${resolverKClass.qualifiedName}' via ${resolverClass.classLoader}"
                )
                val resolverExecutor = FieldBatchResolverExecutorImpl(
                    objectSelectionSet = objectSelectionSet,
                    querySelectionSet = querySelectionSet,
                    resolver = resolverContainerProvider,
                    batchResolveFn = batchResolveFunction,
                    resolverId = formattedResolverId,
                    globalIDCodec = globalIDCodec,
                    reflectionLoader = reflectionLoader,
                    resolverContextFactory = resolverContextFactory,
                )
                result.put(resolverId, resolverExecutor)?.let { extant ->
                    throw RuntimeException(
                        "Duplicate resolver for type $typeName and field $fieldName. " +
                            "Found $extant in class '${resolverKClass.qualifiedName}'."
                    )
                }
            }
        }
        return result.entries.map { it.key to it.value }
    }

    /**
     * For each node resolver in this tenant module, creates a [NodeUnbatchedResolverExecutor] or [NodeResolverExecutor] depending
     * on whether the tenant implements `resolve` or `batchResolve`. Returns these as maps from the node name to the
     * corresponding object.
     */
    override fun nodeResolverExecutors(): Iterable<Pair<String, NodeResolverExecutor>> {
        val nodeResolverExecutors: MutableMap<String, NodeResolverExecutor> = mutableMapOf()
        val nodeResolverBase = NodeResolverBase::class.java

        // Get all classes annotated with @NodeResolverFor in TenantModule's package
        val nodeResolverForClasses: Set<Class<*>> = tenantResolverClassFinder.nodeResolverForClassesInPackage()
        val nodeResolverBaseClasses: List<Class<out NodeResolverBase<*>>> =
            nodeResolverForClasses.map {
                if (!nodeResolverBase.isAssignableFrom(it)) {
                    throw TenantModuleException("Found @NodeResolverFor on class that doesn't implement NodeResolverBase ($it)")
                }
                it.asSubclass(nodeResolverBase)
            }
        @Suppress("UNCHECKED_CAST")
        val nodeResolverClassesByBaseClass: Map<Class<out NodeResolverBase<*>>, Set<Class<out NodeResolverBase<*>>>> =
            nodeResolverBaseClasses.associateWith { // Get all node resolver subclasses
                tenantResolverClassFinder.getSubTypesOf(it.name) as Set<Class<out NodeResolverBase<*>>>
            }
        nodeResolverClassesByBaseClass.forEach { (baseClass, nodeResolverClasses) ->
            val nodeResolverForAnnotation = baseClass.annotations.first { it is NodeResolverFor } as NodeResolverFor
            val typeName = nodeResolverForAnnotation.typeName
            if (nodeResolverClasses.size != 1) {
                throw TenantModuleException(
                    "Expected exactly one resolver implementation for $typeName, " +
                        "found ${nodeResolverClasses.size}: ${nodeResolverClasses.map { it.name }}"
                )
            }
            val resolverClass = nodeResolverClasses.first()

            // We register providers for the resolvers here, and this is the only place where we discover Resolver classes
            // for Viaduct, register their providers with the injector provided in the constructor for this class,
            // and create the resolver executors for Viaduct engine.
            val resolverContainerProvider = try {
                tenantCodeInjector.getProvider(resolverClass)
            } catch (e: NoClassDefFoundError) {
                // This can happen, at times, for tenant JARs whose dependencies don't resolve.
                // By re-throwing a TenantModuleException, we ensure we only skip the bootstrapping of one offending tenant.
                throw TenantModuleException("Resolver class $resolverClass could not be injected into", e)
            }
            val resolverKClass = resolverClass.kotlin
            val resolveFunction = resolverKClass.declaredMemberFunctions.firstOrNull { it.name == "resolve" }
            val batchResolveFunction = resolverKClass.declaredMemberFunctions.firstOrNull { it.name == "batchResolve" }

            @Suppress("UNCHECKED_CAST")
            val contextKClass = baseClass.declaredClasses.firstOrNull {
                NodeExecutionContext::class.java.isAssignableFrom(it)
            }?.kotlin as? KClass<NodeExecutionContext<NodeObject>>
                ?: throw java.lang.IllegalArgumentException("No nested Context class found in ${baseClass.name}")
            val resolverContextFactory = NodeResolverContextFactory.forClass(
                contextKClass,
                NodeExecutionContextMetaFactory.create(
                    selections = SelectionSetContextFactory.forTypeName(typeName),
                )
            )

            if (resolveFunction != null) {
                if (batchResolveFunction != null) {
                    throw TenantModuleException("Resolver class $resolverKClass implements both 'resolve' and 'batchResolve', it should only implement one")
                }
                log.info(
                    "- Adding node resolver entry for '$typeName' " +
                        "to '${resolverKClass.qualifiedName}'."
                )
                val nodeUnbatchedResolverExecutor =
                    NodeUnbatchedResolverExecutorImpl(
                        resolver = resolverContainerProvider,
                        resolveFunction = resolveFunction,
                        typeName = typeName,
                        globalIDCodec = globalIDCodec,
                        reflectionLoader = reflectionLoader,
                        factory = resolverContextFactory,
                    )
                nodeResolverExecutors.put(typeName, nodeUnbatchedResolverExecutor)?.let { extant ->
                    throw TenantModuleException(
                        "Duplicate node resolver for type $typeName. " +
                            "Found $extant in class '${resolverKClass.qualifiedName}'."
                    )
                }
            } else if (batchResolveFunction != null) {
                log.info(
                    "- Adding node batch resolver entry for '$typeName' " +
                        "to '${resolverKClass.qualifiedName}'."
                )
                val nodeResolverExecutor =
                    NodeBatchResolverExecutorImpl(
                        resolver = resolverContainerProvider,
                        batchResolveFunction = batchResolveFunction,
                        typeName = typeName,
                        globalIDCodec = globalIDCodec,
                        reflectionLoader = reflectionLoader,
                        factory = resolverContextFactory,
                    )
                nodeResolverExecutors.put(typeName, nodeResolverExecutor)?.let { extant ->
                    throw TenantModuleException(
                        "Duplicate batch node resolver for type $typeName. " +
                            "Found $extant in class '${resolverKClass.qualifiedName}'."
                    )
                }
            } else {
                throw TenantModuleException("Resolver class $resolverKClass implements neither 'resolve' nor 'batchResolve'")
            }
        }
        return nodeResolverExecutors.entries.map { it.key to it.value }
    }

    companion object {
        private val log by logger()

        private fun formatResolverId(typeFieldTuple: Pair<String, String>): String = "${typeFieldTuple.first}.${typeFieldTuple.second}"

        /**
         * Finds the argument-type class for inputs of the resolver method for a given (typeName, fieldName) coordinate.
         *
         * @param typeName Viaduct type name as a string.
         * @param fieldName Field name for type [typeName] as a string.
         * @return KClass<out viaduct.api.types.Arguments> corresponding to type-fieldname coordinate.
         *
         */
        fun findArgumentsClass(
            classFinder: TenantResolverClassFinder,
            typeName: String,
            fieldName: String
        ): KClass<out Arguments> = classFinder.argumentClassForName(argumentTypeName(typeName, fieldName))

        // Utility function to get argumentTypeName based of type and field names.
        private fun argumentTypeName(
            typeName: String,
            fieldName: String
        ): String = "${typeName}_${fieldName.capitalize()}_Arguments"
    }

    private val Class<*>.isKotlinClass
        get() = this.isAnnotationPresent(Metadata::class.java)
}
