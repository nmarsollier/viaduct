package viaduct.engine.api

/**
 * Loads selections from the engine and return an engine object data.
 */
interface RawSelectionsLoader {
    /**
     * Given a [RawSelectionSet], load from the engine and return an [EngineObjectData].
     * @param selections The raw selections to load.
     * @return The engine object data.
     */
    suspend fun load(selections: RawSelectionSet): EngineObjectData

    /**
     * A factory for creating [RawSelectionsLoader] instances.
     */
    interface Factory {
        /**
         * Create a [RawSelectionsLoader] for a query.
         */
        fun forQuery(resolverId: String): RawSelectionsLoader

        /**
         * Create a [RawSelectionsLoader] for a mutation.
         */
        fun forMutation(resolverId: String): RawSelectionsLoader
    }
}
