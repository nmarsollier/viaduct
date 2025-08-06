package viaduct.testapps.schemaregistration.tenant2

import viaduct.api.TenantModule

class Tenant2Module : TenantModule {
    override val metadata = mapOf(
        "name" to "Tenant2"
    )
}
