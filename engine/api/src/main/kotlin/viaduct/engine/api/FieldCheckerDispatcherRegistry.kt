package viaduct.engine.api

interface FieldCheckerDispatcherRegistry {
    /**
     * Get a [CheckerExecutor] for a provided typeName-fieldName coordinate,
     * or null if no CheckerExecutor is registered for the given coordinate.
     */
    fun getCheckerExecutor(
        typeName: String,
        fieldName: String
    ): CheckerExecutor?

    /** A [FieldCheckerDispatcherRegistry] that returns null for every request */
    object Empty : FieldCheckerDispatcherRegistry {
        override fun getCheckerExecutor(
            typeName: String,
            fieldName: String
        ): CheckerExecutor? = null
    }
}
