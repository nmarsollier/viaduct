package viaduct.tenant.runtime.bootstrap

fun interface TenantPackageFinder {
    /**
     * Returns a set of all tenant modules to consider in a given context such as an executor registry.
     * Each tenant module is uniquely identified by its Java package name.
     */
    fun tenantPackages(): Set<String>
}
