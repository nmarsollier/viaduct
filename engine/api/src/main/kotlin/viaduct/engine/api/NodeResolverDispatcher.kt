package viaduct.engine.api

/**
 * Class that delegates to a data loader to call [NodeResolverExecutor]
 */
interface NodeResolverDispatcher {
    suspend fun resolve(
        id: String,
        selections: RawSelectionSet,
        context: EngineExecutionContext
    ): EngineObjectData
}
