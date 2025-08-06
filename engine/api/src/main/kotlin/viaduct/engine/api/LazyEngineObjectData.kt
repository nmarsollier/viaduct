package viaduct.engine.api

/**
 * An EngineObjectData that is not fully resolved until [resolveData] is called.
 */
interface LazyEngineObjectData : EngineObjectData {
    suspend fun resolveData(
        selections: RawSelectionSet,
        context: EngineExecutionContext
    )
}
