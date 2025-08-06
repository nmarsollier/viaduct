package viaduct.testapps.fixtures

import kotlin.reflect.KClass
import viaduct.api.TenantModule
import viaduct.tenant.runtime.bootstrap.TenantPackageFinder

/**
 * A mock implementations of the TenantPackageFinder interface for testing.
 */
class TestTenantPackageFinder(classes: Iterable<KClass<out TenantModule>>) : TenantPackageFinder {
    private val packages = classes.map { it.java.packageName }.toSet()

    override fun tenantPackages() = packages
}
