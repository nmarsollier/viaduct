package viaduct.engine.api

interface TypeCheckerDispatcherRegistry {
    /**
     * Get a [CheckerExecutor] for the provided typeName, or null if there is no
     * CheckerExecutor registered for the given typeName.
     */
    fun getTypeCheckerExecutor(typeName: String): CheckerExecutor?

    /** A [TypeCheckerDispatcherRegistry] that returns null for every request */
    object Empty : TypeCheckerDispatcherRegistry {
        override fun getTypeCheckerExecutor(typeName: String): CheckerExecutor? = null
    }
}
