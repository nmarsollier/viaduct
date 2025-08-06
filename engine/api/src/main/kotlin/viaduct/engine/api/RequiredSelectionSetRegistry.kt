package viaduct.engine.api

interface RequiredSelectionSetRegistry {
    /**
     * Get a list of [RequiredSelectionSet] for a provided typeName-fieldName coordinate.
     * If the coordinate has no RequiredSelectionSet, it'll be an empty list.
     */
    fun getRequiredSelectionSets(
        typeName: String,
        fieldName: String,
        executeAccessChecksInModstrat: Boolean
    ): List<RequiredSelectionSet>

    /** A [RequiredSelectionSetRegistry] that returns empty list for every request */
    object Empty : RequiredSelectionSetRegistry {
        override fun getRequiredSelectionSets(
            typeName: String,
            fieldName: String,
            executeAccessChecksInModstrat: Boolean
        ): List<RequiredSelectionSet> = emptyList()
    }
}
