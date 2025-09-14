package viaduct.demoapp.tenant1

import viaduct.api.TenantModule

class HelloWorldTenantModule : TenantModule {
    override val metadata = mapOf(
        "name" to "HelloWorldTenantModule"
    )
}
