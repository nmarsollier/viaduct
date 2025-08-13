package viaduct.tenant.runtime.context.factory

import viaduct.api.context.NodeExecutionContext
import viaduct.api.context.ResolverExecutionContext
import viaduct.api.globalid.GlobalID
import viaduct.api.internal.InternalContext
import viaduct.api.internal.select.SelectionsLoader
import viaduct.api.select.SelectionSet
import viaduct.api.types.NodeObject
import viaduct.engine.api.EngineExecutionContext
import viaduct.engine.api.RawSelectionSet
import viaduct.tenant.runtime.context.NodeExecutionContextImpl

/** A collection of args that are used by a NodeExecutionContext [Factory] */
class NodeArgs(
    val internalContext: InternalContext,
    val globalID: String,
    val resolverId: String,
    val selectionSetFactory: viaduct.api.internal.select.SelectionSetFactory,
    val selections: RawSelectionSet?,
    val selectionsLoaderFactory: SelectionsLoader.Factory,
    val engineExecutionContext: EngineExecutionContext
)

fun interface NodeExecutionContextFactory {
    fun make(args: NodeArgs): NodeExecutionContext<*>
}

@Suppress("UNCHECKED_CAST")
object NodeExecutionContextMetaFactory {
    /** Create a [Factory<Args, NodeExecutionContext>] assembled from the provided factories */
    fun create(
        selections: Factory<SelectionSetArgs, SelectionSet<*>>,
        globalID: Factory<GlobalIDArgs, GlobalID<*>> = GlobalIDFactory.default,
        executionContext: Factory<ResolverExecutionContextArgs, ResolverExecutionContext> = ResolverExecutionContextFactory.default,
    ): NodeExecutionContextFactory =
        NodeExecutionContextFactory { args ->
            NodeExecutionContextImpl(
                executionContext(
                    ResolverExecutionContextArgs(
                        internalContext = args.internalContext,
                        selectionSetFactory = args.selectionSetFactory,
                        resolverId = args.resolverId,
                        selectionsLoaderFactory = args.selectionsLoaderFactory,
                        engineExecutionContext = args.engineExecutionContext,
                    )
                ),
                globalID(
                    GlobalIDArgs(
                        internalContext = args.internalContext,
                        globalID = args.globalID,
                    )
                ) as GlobalID<NodeObject>,
                selections(
                    SelectionSetArgs(
                        internalContext = args.internalContext,
                        selections = args.selections,
                    )
                ) as SelectionSet<NodeObject>,
            )
        }
}
