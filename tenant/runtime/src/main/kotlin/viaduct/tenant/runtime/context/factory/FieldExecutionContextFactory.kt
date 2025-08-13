package viaduct.tenant.runtime.context.factory

import viaduct.api.context.FieldExecutionContext
import viaduct.api.context.ResolverExecutionContext
import viaduct.api.internal.InternalContext
import viaduct.api.internal.select.SelectionSetFactory as InternalSelectionSetFactory
import viaduct.api.internal.select.SelectionsLoader
import viaduct.api.select.SelectionSet
import viaduct.api.types.Arguments
import viaduct.api.types.Object
import viaduct.api.types.Query
import viaduct.engine.api.EngineExecutionContext
import viaduct.engine.api.EngineObjectData
import viaduct.engine.api.RawSelectionSet
import viaduct.tenant.runtime.context.FieldExecutionContextImpl

/** A collection of Args that are used by a FieldExecutionContext [Factory] */
data class FieldArgs(
    val internalContext: InternalContext,
    val arguments: Map<String, Any?>,
    val objectValue: EngineObjectData,
    val queryValue: EngineObjectData,
    val resolverId: String,
    val selectionSetFactory: InternalSelectionSetFactory,
    val selections: RawSelectionSet?,
    val selectionsLoaderFactory: SelectionsLoader.Factory,
    val engineExecutionContext: EngineExecutionContext,
)

fun interface FieldExecutionContextFactory {
    fun make(args: FieldArgs): FieldExecutionContext<*, *, *, *>
}

object FieldExecutionContextMetaFactory {
    /**
     * Create a [Factory<FieldArgs, FieldExecutionContextImpl>] that returns [FieldExecutionContextImpl]
     * instances assembled from the provided factories.
     */
    fun create(
        objectValue: Factory<ObjectArgs, Object>,
        queryValue: Factory<ObjectArgs, Query>,
        arguments: Factory<ArgumentsArgs, Arguments>,
        selectionSet: Factory<SelectionSetArgs, SelectionSet<*>>,
        resolverExecutionContext: Factory<ResolverExecutionContextArgs, ResolverExecutionContext> = ResolverExecutionContextFactory.default,
    ): FieldExecutionContextFactory =
        FieldExecutionContextFactory { args ->
            FieldExecutionContextImpl(
                resolverExecutionContext(
                    ResolverExecutionContextArgs(
                        internalContext = args.internalContext,
                        selectionSetFactory = args.selectionSetFactory,
                        resolverId = args.resolverId,
                        selectionsLoaderFactory = args.selectionsLoaderFactory,
                        engineExecutionContext = args.engineExecutionContext,
                    )
                ),
                objectValue.mk(
                    ObjectArgs(
                        internalContext = args.internalContext,
                        objectValue = args.objectValue,
                    )
                ),
                queryValue.mk(
                    ObjectArgs(
                        internalContext = args.internalContext,
                        objectValue = args.queryValue,
                    )
                ),
                arguments.mk(
                    ArgumentsArgs(
                        internalContext = args.internalContext,
                        arguments = args.arguments,
                    )
                ),
                selectionSet.mk(
                    SelectionSetArgs(
                        internalContext = args.internalContext,
                        selections = args.selections,
                    )
                )
            )
        }
}
