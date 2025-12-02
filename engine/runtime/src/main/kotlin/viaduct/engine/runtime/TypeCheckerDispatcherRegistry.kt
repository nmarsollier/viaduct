package viaduct.engine.runtime

import viaduct.engine.api.CheckerDispatcher

interface TypeCheckerDispatcherRegistry {
    /**
     * Get a [CheckerDispatcher] for the provided typeName, or null if there is no
     * CheckerDispatcher registered for the given typeName.
     */
    fun getTypeCheckerDispatcher(typeName: String): CheckerDispatcher?

    /** A [TypeCheckerDispatcherRegistry] that returns null for every request */
    object Empty : TypeCheckerDispatcherRegistry {
        override fun getTypeCheckerDispatcher(typeName: String): CheckerDispatcher? = null
    }
}
