package viaduct.tenant.runtime.context.factory

import viaduct.api.context.ExecutionContext
import viaduct.api.internal.InternalContext
import viaduct.api.internal.NodeReferenceFactory
import viaduct.api.internal.select.SelectionSetFactory
import viaduct.api.internal.select.SelectionsLoader
import viaduct.api.types.Query
import viaduct.engine.api.EngineExecutionContext
import viaduct.tenant.runtime.context.ExecutionContextImpl

class ExecutionContextArgs(
    val internalContext: InternalContext,
    /** A service-scoped [SelectionSetFactory] */
    val selectionSetFactory: SelectionSetFactory,
    val engineExecutionContext: EngineExecutionContext,
    val resolverId: String,
    /** A service-scoped [SelectionsLoader.Factory] */
    val selectionsLoaderFactory: SelectionsLoader.Factory,
)

object ExecutionContextFactory {
    /** create a [Factory<Args, ExecutionContext>] assembled from the provided factories */
    fun create(
        queryLoader: Factory<SelectionsLoaderArgs, SelectionsLoader<Query>> = SelectionsLoaderFactory.forQuery,
        nodeReferenceFactory: Factory<EngineExecutionContext, NodeReferenceFactory> = NodeReferenceContextFactory.default,
    ): Factory<ExecutionContextArgs, ExecutionContext> =
        Factory { args ->
            ExecutionContextImpl(
                args.internalContext,
                queryLoader(SelectionsLoaderArgs(resolverId = args.resolverId, selectionsLoaderFactory = args.selectionsLoaderFactory)),
                args.selectionSetFactory,
                nodeReferenceFactory(args.engineExecutionContext)
            )
        }

    /** A default `Factory<Args, ExecutionContext>` suitable for general use */
    val default: Factory<ExecutionContextArgs, ExecutionContext> = create()
}
