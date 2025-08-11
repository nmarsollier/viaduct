package viaduct.engine.runtime.execution

import graphql.language.Document
import graphql.language.OperationDefinition
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.FieldCoordinates
import java.util.concurrent.CompletableFuture
import kotlin.collections.mapValues
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import viaduct.engine.api.CheckerDispatcher
import viaduct.engine.api.FieldResolverDispatcher
import viaduct.engine.api.FragmentLoader
import viaduct.engine.api.ObjectEngineResult
import viaduct.engine.api.RequiredSelectionSet
import viaduct.engine.api.ResolvedEngineObjectData
import viaduct.engine.api.VariablesResolver
import viaduct.engine.api.coroutines.CoroutineInterop
import viaduct.engine.api.derived.DerivedFieldQueryMetadata
import viaduct.engine.api.fragment.Fragment
import viaduct.engine.api.resolve
import viaduct.engine.runtime.EngineExecutionContextImpl
import viaduct.engine.runtime.EngineResultLocalContext
import viaduct.engine.runtime.ObjectEngineResultImpl
import viaduct.engine.runtime.ProxyEngineObjectData
import viaduct.engine.runtime.execution.FieldExecutionHelpers.resolveVariables
import viaduct.engine.runtime.findLocalContextForType
import viaduct.engine.runtime.getLocalContextForType
import viaduct.service.api.spi.FlagManager
import viaduct.service.api.spi.Flags

class ResolverDataFetcher(
    internal val typeName: String,
    internal val fieldName: String,
    private val fieldResolverDispatcher: FieldResolverDispatcher,
    private val checkerDispatcher: CheckerDispatcher?,
    private val fragmentLoader: FragmentLoader,
    private val flagManager: FlagManager,
    private val tenantNameResolver: TenantNameResolver,
    private val coroutineInterop: CoroutineInterop = DefaultCoroutineInterop
) : DataFetcher<CompletableFuture<*>> {
    companion object {
        /**
         * Data class to hold the resolver and query proxy engine object data.
         * This is used to resolve the field in the resolver executor.
         */
        private data class EngineObjectData(
            val fieldResolverDispatcherEOD: ProxyEngineObjectData,
            val queryProxyEOD: ProxyEngineObjectData
        )

        /**
         * Data class to hold the results of the engine execution.
         */
        private data class EngineResults(
            val parentResult: ObjectEngineResult,
            val queryResult: ObjectEngineResult
        )
    }

    override fun get(environment: DataFetchingEnvironment): CompletableFuture<*> =
        coroutineInterop.scopedFuture {
            resolve(environment)
        }

    private suspend fun resolve(environment: DataFetchingEnvironment): Any? {
        val engineResults = getEngineResults(environment)

        val engineExecutionContext = environment.findLocalContextForType<EngineExecutionContextImpl>()
        val localExecutionContext = engineExecutionContext.copy(
            dataFetchingEnvironment = environment
        )

        val (objectValueEOD, queryValueEOD) = getFieldResolverDispatcherEOD(localExecutionContext, environment, engineResults)
        if (localExecutionContext.executeAccessChecksInModstrat) {
            return resolveField(environment, objectValueEOD, queryValueEOD, localExecutionContext)
        }

        // Before modern access check is fully implemented, all modern fields will not be using
        // modern execution strategy, thus we still need to execute access check here.
        // TODO: Checker execution below will be removed from data fetcher, once modern strategy
        //  implementation is done.
        // --------- Execute access checks in ResolverDataFetcher ---------------
        // If there is no checker, just resolve the field
        if (checkerDispatcher == null) {
            return resolveField(environment, objectValueEOD, queryValueEOD, localExecutionContext)
        }

        val checkerProxyEODMap = getCheckerProxyEODMap(environment, engineResults.parentResult)
        return when (environment.operationDefinition.operation) {
            // for query, execute checker and resolve field in parallel
            OperationDefinition.Operation.QUERY -> {
                supervisorScope {
                    val checkAsync = async {
                        checkerDispatcher.execute(
                            environment.arguments,
                            checkerProxyEODMap,
                            localExecutionContext
                        )
                    }
                    runCatching {
                        resolveField(environment, objectValueEOD, queryValueEOD, localExecutionContext)
                    }.onSuccess {
                        val checkerResult = checkAsync.await()
                        checkerResult.asError?.let { throw it.error }
                    }.getOrThrow()
                }
            }
            // for mutation, execute checker then resolve field synchronously
            OperationDefinition.Operation.MUTATION -> {
                val checkerResult = checkerDispatcher.execute(
                    environment.arguments,
                    checkerProxyEODMap,
                    localExecutionContext
                )
                checkerResult.asError?.let { throw it.error }
                resolveField(environment, objectValueEOD, queryValueEOD, localExecutionContext)
            }

            else -> throw NotImplementedError("Unsupported operation: ${environment.operationDefinition.operation}")
        }
    }

    private suspend fun getFieldResolverDispatcherEOD(
        localExecutionContext: EngineExecutionContextImpl,
        environment: DataFetchingEnvironment,
        engineResults: EngineResults,
    ): EngineObjectData {
        val selectionSetFactory = localExecutionContext.rawSelectionSetFactory
        val fieldResolverDispatcherEOD = fieldResolverDispatcher.objectSelectionSet?.let { rss ->
            val variables = resolveVariables(engineResults.parentResult, rss, environment.arguments, localExecutionContext)
            ProxyEngineObjectData(
                engineResults.parentResult,
                selectionSetFactory.rawSelectionSet(rss.selections, variables)
            )
        } ?: ProxyEngineObjectData(engineResults.parentResult)
        val queryProxyEOD = fieldResolverDispatcher.querySelectionSet?.let { rss ->
            val variables = resolveVariables(engineResults.queryResult, rss, environment.arguments, localExecutionContext)
            ProxyEngineObjectData(
                engineResults.queryResult,
                selectionSetFactory.rawSelectionSet(rss.selections, variables)
            )
        } ?: ProxyEngineObjectData(engineResults.queryResult)
        return EngineObjectData(fieldResolverDispatcherEOD, queryProxyEOD)
    }

    private suspend fun resolveField(
        environment: DataFetchingEnvironment,
        fieldResolverDispatcherEOD: ProxyEngineObjectData,
        resolverQueryProxyEOD: ProxyEngineObjectData,
        engineExecutionContext: EngineExecutionContextImpl
    ) = fieldResolverDispatcher.resolve(
        environment.arguments,
        fieldResolverDispatcherEOD,
        resolverQueryProxyEOD,
        engineExecutionContext.rawSelectionSetFactory.rawSelectionSet(environment),
        engineExecutionContext
    )

    private fun getQueryMetadata(
        environment: DataFetchingEnvironment,
        fieldCoordinates: FieldCoordinates
    ): DerivedFieldQueryMetadata = createQueryMetadata(environment, fieldCoordinates, onRootQuery = false)

    /**
     * Create metadata for Query fragment loading with onRootQuery=true.
     * Used when resolvers need root query data in legacy execution strategy.
     */
    private fun getQueryMetadataForRoot(environment: DataFetchingEnvironment): DerivedFieldQueryMetadata =
        createQueryMetadata(environment, FieldCoordinates.coordinates(typeName, fieldName), onRootQuery = true)

    /**
     * Create DerivedFieldQueryMetadata with shared logic.
     */
    private fun createQueryMetadata(
        environment: DataFetchingEnvironment,
        fieldCoordinates: FieldCoordinates,
        onRootQuery: Boolean
    ): DerivedFieldQueryMetadata {
        val typePart = fieldCoordinates.typeName
        val fieldPart = fieldCoordinates.fieldName
        val suffix = if (onRootQuery) "QueryResolver" else "Resolver"
        val metadataClassPath = "${typePart}_${fieldPart}_$suffix"
        val rootFieldSuffix = if (onRootQuery) "Query_Root" else "${typePart}_Root"

        return DerivedFieldQueryMetadata(
            queryName = "${metadataClassPath}_Query",
            rootFieldName = "${fieldPart}_$rootFieldSuffix",
            classPath = metadataClassPath,
            providerShortClasspath = metadataClassPath,
            onRootQuery = onRootQuery,
            onRootMutation = false,
            allowMutationOnQuery = false,
            fieldOwningTenant = tenantNameResolver.resolve(environment.fieldDefinition)
        )
    }

    // return true if both the engine is broadly enabled for the given operation type *AND* execution
    // of modern fields is enabled on modstrat
    private fun shouldUseModernExecutionStrategy(environment: DataFetchingEnvironment): Boolean =
        flagManager.isEnabled(Flags.useModernExecutionStrategy(environment.operationDefinition.operation)) &&
            flagManager.isEnabled(Flags.USE_MODERN_EXECUTION_STRATEGY_FOR_MODERN_FIELDS)

    private suspend fun getEngineResults(environment: DataFetchingEnvironment): EngineResults =
        coroutineScope {
            // Get query result - use dedicated queryEngineResult for modern strategy or when no query selections
            val queryEngineResultDeferred = if (shouldUseModernExecutionStrategy(environment)) {
                val engineLoaderContext = environment.findLocalContextForType<EngineResultLocalContext>()
                // Modern strategy - use the queryEngineResult from context
                CompletableDeferred(
                    engineLoaderContext.queryEngineResult
                        ?: throw IllegalStateException("Missing query ObjectEngineResult")
                )
            } else {
                if (fieldResolverDispatcher.querySelectionSet == null) {
                    // Legacy strategy with no query selections - use empty queryEngineResult from context
                    CompletableDeferred(ObjectEngineResultImpl.newForType(environment.graphQLSchema.queryType))
                } else {
                    // Legacy strategy with query selections - still need to fetch data using fragment loader
                    async {
                        fragmentLoader.loadEngineObjectData(
                            buildQueryFragment(environment, fieldResolverDispatcher.querySelectionSet!!),
                            getQueryMetadataForRoot(environment),
                            mapOf<String, Any?>(), // Root query has no source object
                            environment
                        ) as ObjectEngineResult
                    }
                }
            }

            val parentEngineResultDeferred = if (shouldUseModernExecutionStrategy(environment)) {
                val engineLoaderContext = environment.findLocalContextForType<EngineResultLocalContext>()
                CompletableDeferred(
                    engineLoaderContext.parentEngineResult
                        ?: throw IllegalStateException("Missing parent ObjectEngineResult")
                )
            } else {
                val resolverSelectionSet = fieldResolverDispatcher.objectSelectionSet
                val checkerSelectionSets = checkerDispatcher?.requiredSelectionSets?.values?.toList()?.filterNotNull()
                if (resolverSelectionSet == null && checkerSelectionSets.isNullOrEmpty()) {
                    CompletableDeferred(ObjectEngineResultImpl.newForType(environment.executionStepInfo.objectType))
                } else {
                    async {
                        fragmentLoader.loadEngineObjectData(
                            buildFragment(environment, resolverSelectionSet, checkerSelectionSets),
                            getQueryMetadata(environment, FieldCoordinates.coordinates(typeName, fieldName)),
                            environment.getSource()!!,
                            environment
                        ) as ObjectEngineResult
                    }
                }
            }
            val queryEngineResult = queryEngineResultDeferred.await()
            val parentEngineResult = parentEngineResultDeferred.await()

            assert(parentEngineResult.graphQLObjectType.name == typeName)

            EngineResults(parentEngineResult, queryEngineResult)
        }

    /**
     * Get checker proxyEOD from engine result. This supports both old and new engine.
     * Note if `shouldUseModernExecutionStrategy(...)` returns false, it means the engine result
     * is loaded from old engine, hence only builds proxyEOD from the first checker selection set.
     */
    private fun getCheckerProxyEODMap(
        environment: DataFetchingEnvironment,
        engineResult: ObjectEngineResult
    ): Map<String, ProxyEngineObjectData> {
        check(checkerDispatcher != null) {
            "Checker executor should not be null when getting checker proxyEOD map."
        }
        val checkerSelectionSetMap = if (shouldUseModernExecutionStrategy(environment)) {
            checkerDispatcher.requiredSelectionSets
        } else {
            // Get checker proxyEOD from old engine.
            // Note checker variables and composite checkers with multiple selection sets are not supported.
            val firstRss = checkerDispatcher.requiredSelectionSets.values.firstOrNull()
                ?: return mapOf(
                    "key" to ProxyEngineObjectData(engineResult)
                )
            mapOf(
                "key" to firstRss
            )
        }

        val selectionSetFactory =
            environment.findLocalContextForType<EngineExecutionContextImpl>().rawSelectionSetFactory
        return checkerSelectionSetMap.mapValues { (_, rss) ->
            rss?.let {
                ProxyEngineObjectData(
                    engineResult,
                    selectionSetFactory.rawSelectionSet(rss.selections, emptyMap())
                )
            } ?: ProxyEngineObjectData(engineResult)
        }
    }

    /**
     * Build a fragment from both resolver and checker selection sets for fragment loader to load.
     * Note: this is only used in old engine, so only support single checker selection set to load himeji id.
     */
    private suspend fun buildFragment(
        environment: DataFetchingEnvironment,
        resolverSelectionSet: RequiredSelectionSet?,
        checkerSelectionSets: List<RequiredSelectionSet>?
    ): Fragment {
        // Checker variables are not supported in old engine.
        val engineExecCtx = environment.findLocalContextForType<EngineExecutionContextImpl>()

        // when modstrat is not enabled, we have no EngineResultLocalContext from which we can access an ObjectEngineResult
        // For now, pass an empty EngineObjectData to the VariablesResolver, in lieu of a proper ProxyEngineObjectData
        // backed by the VariablesResolver's required selection set.
        // This will not allow use of FromField variables. We expect that we will be fully running on modstrat before
        // FromField support is needed in this code path.
        val emptyEngineData = ResolvedEngineObjectData
            .Builder(environment.executionStepInfo.objectType)
            .build()
        val variablesMap = if (resolverSelectionSet != null) {
            val variables = resolverSelectionSet.variablesResolvers.resolve(
                VariablesResolver.ResolveCtx(
                    emptyEngineData,
                    environment.arguments,
                    engineExecCtx
                )
            )
            variables.filterValues { it != null }.mapValues { it.value!! }
        } else {
            emptyMap()
        }

        val resolverFragmentDefs = resolverSelectionSet?.selections?.fragmentMap?.values?.toList()
        // Only support single checker selection set to load himeji id from the same typeName in old engine.
        val checkerFragmentDefs = checkerSelectionSets?.firstOrNull()?.selections?.fragmentMap?.values?.toList()
        val fragmentDefs = if (resolverFragmentDefs != null && checkerFragmentDefs != null) {
            // add checker selection set to resolver selection set
            val topLevelResolverFragmentDef = resolverFragmentDefs.first { it.typeCondition.name == typeName }
            val topLevelCheckerFragmentDef = checkerFragmentDefs.first { it.typeCondition.name == typeName }
            val restOfCheckerFragmentDefs = checkerFragmentDefs.filter { it != topLevelCheckerFragmentDef }
            val originalSelections = topLevelResolverFragmentDef.selectionSet.selections
            val updatedSelectionSet = topLevelResolverFragmentDef.selectionSet.transform {
                it.selections(originalSelections + topLevelCheckerFragmentDef.selectionSet.selections).build()
            }
            resolverFragmentDefs.map {
                if (it == topLevelResolverFragmentDef) {
                    it.transform { fragmentDef -> fragmentDef.selectionSet(updatedSelectionSet).build() }
                } else {
                    it
                }
            } + restOfCheckerFragmentDefs
        } else {
            resolverFragmentDefs ?: checkerFragmentDefs
        }

        val doc = Document.newDocument()
            .definitions(fragmentDefs)
            .build()
        return Fragment(doc, variablesMap)
    }

    /**
     * Build a fragment specifically for Query type selections.
     * Used when resolvers need root query data in legacy execution strategy.
     */
    private suspend fun buildQueryFragment(
        environment: DataFetchingEnvironment,
        querySelectionSet: RequiredSelectionSet
    ): Fragment {
        val engineExecCtx = environment.findLocalContextForType<EngineExecutionContextImpl>()
        val queryEngineResult = environment.getLocalContextForType<EngineResultLocalContext>()?.queryEngineResult
            ?: ObjectEngineResultImpl.newForType(environment.graphQLSchema.queryType)
        val variablesMap = resolveVariables(queryEngineResult, querySelectionSet, environment.arguments, engineExecCtx)
            .filterValues { it != null }.mapValues { it.value!! }

        val fragmentDefs = querySelectionSet.selections.fragmentMap.values.toList()
        val doc = Document.newDocument().definitions(fragmentDefs).build()
        return Fragment(doc, variablesMap)
    }
}
