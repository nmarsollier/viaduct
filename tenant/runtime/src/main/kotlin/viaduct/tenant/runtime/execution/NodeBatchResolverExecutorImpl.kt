package viaduct.tenant.runtime.execution

import javax.inject.Provider
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import viaduct.api.FieldValue
import viaduct.api.ViaductFrameworkException
import viaduct.api.ViaductTenantResolverException
import viaduct.api.globalid.GlobalIDCodec
import viaduct.api.internal.NodeResolverBase
import viaduct.api.internal.ReflectionLoader
import viaduct.api.wrapResolveException
import viaduct.engine.api.EngineExecutionContext
import viaduct.engine.api.EngineObjectData
import viaduct.engine.api.NodeResolverExecutor
import viaduct.tenant.runtime.context.factory.NodeExecutionContextFactory

class NodeBatchResolverExecutorImpl(
    val resolver: Provider<out NodeResolverBase<*>>,
    private val batchResolveFunction: KFunction<*>,
    override val typeName: String,
    private val globalIDCodec: GlobalIDCodec,
    private val reflectionLoader: ReflectionLoader,
    private val factory: NodeExecutionContextFactory,
) : NodeResolverExecutor {
    override val isBatching = true

    override suspend fun batchResolve(
        selectors: List<NodeResolverExecutor.Selector>,
        context: EngineExecutionContext
    ): Map<NodeResolverExecutor.Selector, Result<EngineObjectData>> {
        val contexts = selectors.map { key ->
            factory(context, key.selections, context.requestContext, key.id)
        }
        val resolver = resolver.get()
        val results = wrapResolveException(typeName) {
            batchResolveFunction.callSuspend(resolver, contexts)
        }
        if (results !is List<*>) {
            throw IllegalStateException("Unexpected return value from batchResolve function for node $typeName: $results")
        }
        if (selectors.size != results.size) {
            throw ViaductTenantResolverException(
                IllegalStateException(
                    "The batchResolve function in the Node resolver for $typeName was given a batch of size ${selectors.size} but returned ${results.size} elements"
                ),
                typeName
            )
        }
        return selectors.zip(results.map { unwrap(it) }).toMap()
    }

    private fun unwrap(fieldValue: Any?): Result<EngineObjectData> {
        if (fieldValue !is FieldValue<*>) {
            throw IllegalStateException("Unexpected result type that is not a FieldValue: $fieldValue")
        }

        try {
            val result = fieldValue.get()
            return Result.success(NodeUnbatchedResolverExecutorImpl.unwrapNodeResolverResult(result))
        } catch (e: Exception) {
            if (e is ViaductFrameworkException) return Result.failure(e)
            return Result.failure(ViaductTenantResolverException(e, typeName))
        }
    }
}
