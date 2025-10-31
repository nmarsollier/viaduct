package viaduct.tenant.runtime.execution

import javax.inject.Provider
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import viaduct.api.FieldValue
import viaduct.api.ViaductFrameworkException
import viaduct.api.ViaductTenantResolverException
import viaduct.api.globalid.GlobalIDCodec
import viaduct.api.internal.ReflectionLoader
import viaduct.api.internal.ResolverBase
import viaduct.api.wrapResolveException
import viaduct.engine.api.EngineExecutionContext
import viaduct.engine.api.FieldResolverExecutor
import viaduct.engine.api.FieldResolverExecutor.Selector
import viaduct.engine.api.RequiredSelectionSet
import viaduct.tenant.runtime.context.factory.FieldArgs
import viaduct.tenant.runtime.context.factory.FieldExecutionContextFactory
import viaduct.tenant.runtime.internal.InternalContextImpl
import viaduct.tenant.runtime.select.SelectionSetFactoryImpl
import viaduct.tenant.runtime.select.SelectionsLoaderImpl

// TODO: import viaduct.tenant.runtime.context2.FieldExecutionContextImpl

/**
 * Executes a tenant-written field resolver's batchResolve function.
 *
 * @param resolverId: Uniquely identifies a resolver function, e.g. "User.fullName" identifies
 * the field resolver for the "fullName" field on the "User" type. This is used for observability.
 */
class FieldBatchResolverExecutorImpl(
    override val objectSelectionSet: RequiredSelectionSet?,
    override val querySelectionSet: RequiredSelectionSet?,
    internal val resolver: Provider<out @JvmSuppressWildcards ResolverBase<*>>, // internal for testing
    private val batchResolveFn: KFunction<*>,
    override val resolverId: String,
    private val globalIDCodec: GlobalIDCodec,
    private val reflectionLoader: ReflectionLoader,
    private val resolverContextFactory: FieldExecutionContextFactory,
) : FieldResolverExecutor {
    override val metadata: Map<String, String> = mapOf(
        "flavor" to "modern",
    )

    override val isBatching = true

    override suspend fun batchResolve(
        selectors: List<Selector>,
        context: EngineExecutionContext
    ): Map<Selector, Result<Any?>> {
        val contexts = selectors.map { key ->
/*
            FieldExecutionContextImpl<*,*,*,*>(
                schema = context.fullSchema,
                globalIDCodec = globalIDCodec,
                reflectionLoader = reflectionLoader,
                arguments = key.arguments,
                objectValue = key.objectValue,
                queryValue = key.queryValue,
                selections = key.selections,
                engineExecutionContext = context,
            )
*/
            resolverContextFactory.make(
                FieldArgs(
                    internalContext = InternalContextImpl(context.fullSchema, globalIDCodec, reflectionLoader),
                    arguments = key.arguments,
                    objectValue = key.objectValue,
                    queryValue = key.queryValue,
                    resolverId = resolverId,
                    selectionSetFactory = SelectionSetFactoryImpl(context.rawSelectionSetFactory),
                    selections = key.selections,
                    selectionsLoaderFactory = SelectionsLoaderImpl.Factory(context.rawSelectionsLoaderFactory),
                    engineExecutionContext = context,
                )
            )
        }
        val resolver = resolver.get()
        val results = wrapResolveException(resolverId) {
            batchResolveFn.callSuspend(resolver, contexts)
        }
        if (results !is List<*>) {
            throw IllegalStateException("Unexpected return value from batchResolve function for field $resolverId: $results")
        }
        if (selectors.size != results.size) {
            throw ViaductTenantResolverException(
                IllegalStateException(
                    "The batchResolve function in the field resolver for $resolverId was given a batch of size ${selectors.size} but returned ${results.size} elements"
                ),
                resolverId
            )
        }
        return selectors.zip(results.map { unwrap(it) }).toMap()
    }

    private fun unwrap(fieldValue: Any?): Result<Any?> {
        if (fieldValue !is FieldValue<*>) {
            throw IllegalStateException("Unexpected result type that is not a FieldValue: $fieldValue")
        }

        try {
            return Result.success(FieldUnbatchedResolverExecutorImpl.unwrapFieldResolverResult(fieldValue.get(), globalIDCodec))
        } catch (e: Exception) {
            if (e is ViaductFrameworkException) return Result.failure(e)
            return Result.failure(ViaductTenantResolverException(e, resolverId))
        }
    }
}
