package viaduct.tenant.runtime.execution

import javax.inject.Provider
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import viaduct.api.globalid.GlobalIDCodec
import viaduct.api.internal.ObjectBase
import viaduct.api.internal.ReflectionLoader
import viaduct.api.internal.ResolverBase
import viaduct.engine.api.EngineExecutionContext
import viaduct.engine.api.EngineObjectData
import viaduct.engine.api.FieldResolverExecutor
import viaduct.engine.api.RawSelectionSet
import viaduct.engine.api.RequiredSelectionSet
import viaduct.tenant.runtime.context.factory.FieldArgs
import viaduct.tenant.runtime.context.factory.FieldExecutionContextFactory
import viaduct.tenant.runtime.internal.InternalContextImpl
import viaduct.tenant.runtime.select.SelectionSetFactoryImpl
import viaduct.tenant.runtime.select.SelectionsLoaderImpl

/**
 * Main ResolverExecutor class that is initialized via ResolverRegistry and is used to
 * execute the resolver function for each (typename, fieldname) tuple.
 *
 * @param resolverId: Uniquely identifies a resolver function, e.g. "User.fullName" identifies
 * the field resolver for the "fullName" field on the "User" type. This is used for observability.
 */
class FieldResolverExecutorImpl(
    override val objectSelectionSet: RequiredSelectionSet?,
    override val querySelectionSet: RequiredSelectionSet?,
    internal val resolver: Provider<out @JvmSuppressWildcards ResolverBase<*>>, // internal for testing
    private val resolverResolveFn: KFunction<*>,
    private val resolverId: String,
    private val globalIDCodec: GlobalIDCodec,
    private val reflectionLoader: ReflectionLoader,
    private val resolverContextFactory: FieldExecutionContextFactory,
) : FieldResolverExecutor {
    override val metadata: Map<String, String> = mapOf(
        "flavor" to "modern",
    )

    override suspend fun resolve(
        arguments: Map<String, Any?>,
        objectValue: EngineObjectData,
        queryValue: EngineObjectData,
        selections: RawSelectionSet?,
        context: EngineExecutionContext,
    ): Any? {
        val ctx = resolverContextFactory.make(
            FieldArgs(
                internalContext = InternalContextImpl(context.fullSchema.schema, globalIDCodec, reflectionLoader),
                arguments = arguments,
                objectValue = objectValue,
                queryValue = queryValue,
                resolverId = resolverId,
                selectionSetFactory = SelectionSetFactoryImpl(context.rawSelectionSetFactory),
                selections = selections,
                selectionsLoaderFactory = SelectionsLoaderImpl.Factory(context.rawSelectionsLoaderFactory),
                engineExecutionContext = context,
            )
        )
        val resolver = mkResolver()
        val result = wrapResolveException(resolverId) {
            resolverResolveFn.callSuspend(resolver, ctx)
        }
        return unwrap(result)
    }

    // public for testing
    fun mkResolver(): ResolverBase<*> = resolver.get()

    private fun unwrap(result: Any?): Any? {
        return when (result) {
            is ObjectBase -> result.unwrap()
            is List<*> -> result.map { unwrap(it) }
            else -> result
        }
    }
}
