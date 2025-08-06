package viaduct.engine.api

interface NodeResolverDispatcherRegistry {
    /**
     * Get a [NodeResolverDispatcher] for the provided typeName, or null if there is no
     * NodeResolverDispatcher registered for the given typeName.
     */
    fun getNodeResolverDispatcher(typeName: String): NodeResolverDispatcher?
}
