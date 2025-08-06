package viaduct.engine.runtime

import viaduct.engine.api.EngineExecutionContext
import viaduct.engine.api.EngineObjectData
import viaduct.engine.api.FieldResolverDispatcher
import viaduct.engine.api.FieldResolverExecutor
import viaduct.engine.api.RawSelectionSet
import viaduct.engine.api.RequiredSelectionSet

/**
 * Initialized via DispathcerRegistry and resolves a single node for a node type whose
 * tenant-written node resolver implements resolve or batch resolve function.
 *
 * If tenant implements the resolve function, it delegates to a dataloader that only
 * does caching and no batching, which dispatches immediately;
 *
 * If tenant implements the batch resolve function, it delegates to a dataloader that
 * handles batching and caching.
 */
class FieldResolverDispatcherImpl(
    private val resolver: FieldResolverExecutor
) : FieldResolverDispatcher {
    override val objectSelectionSet: RequiredSelectionSet? = resolver.objectSelectionSet

    override val querySelectionSet: RequiredSelectionSet? = resolver.querySelectionSet

    override val hasRequiredSelectionSets: Boolean = resolver.hasRequiredSelectionSets()

    override suspend fun resolve(
        arguments: Map<String, Any?>,
        objectValue: EngineObjectData,
        queryValue: EngineObjectData,
        selections: RawSelectionSet?,
        context: EngineExecutionContext,
    ): Any? {
        // TODO: Use a data loader instead of directly calling the resolver executor
        return resolver.resolve(arguments, objectValue, queryValue, selections, context)
    }
}
