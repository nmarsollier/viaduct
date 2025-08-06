package viaduct.testapps.policycheck.tenant1

import viaduct.api.TenantModule

class Tenant1Module : TenantModule {
    override val metadata = mapOf(
        "name" to "Tenant1"
    )
}
