package viaduct.engine.api

/**
 * TenantAPIBootstrapper is a service that provides a list of all TenantModuleBootstrappers
 * that are needed to bootstrap all tenant modules for one flavor of the Tenant API.
 */
interface TenantAPIBootstrapper {
    /**
     *  Provide a list of per-tenant-module bootstrap objects.
     *  The engine will call this once per Tenant API, and will
     *  iterate over the resulting iterator just once. This iterator
     *  is thread-safe to support parallel loading.
     *  @return list of TenantModuleBootstrapper, one per Viaduct tenant module.
     */
    suspend fun tenantModuleBootstrappers(): Iterable<TenantModuleBootstrapper>

    private class Flatten(val items: Iterable<TenantAPIBootstrapper>) : TenantAPIBootstrapper {
        override suspend fun tenantModuleBootstrappers(): Iterable<TenantModuleBootstrapper> = items.flatMap { it.tenantModuleBootstrappers() }
    }

    companion object {
        /** flatten an Iterable of TenantAPIBootstrapper into a single instance */
        fun Iterable<TenantAPIBootstrapper>.flatten(): TenantAPIBootstrapper = Flatten(this)
    }
}
