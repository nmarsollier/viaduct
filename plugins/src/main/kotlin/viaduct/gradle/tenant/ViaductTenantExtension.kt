package viaduct.gradle.tenant

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

/**
 * Viaduct Tenant entry point. This is the gradle extension, where multiple tenants can be created.
 * This configures them in kotlin dsl language with the details of each tenant.
 * viaductTenant {
 *   create("tenant_name") {
 *       packageName.set("tenant_package_name")
 *       schemaDirectory("/src/main/resources")
 *   }
 * }
 */
open class ViaductTenantExtension(private val project: Project) {
    /**
     * The container to hold all tenants directly
     */
    val tenantContainer: NamedDomainObjectContainer<ViaductTenant> =
        project.container(ViaductTenant::class.java) { name ->
            ViaductTenant(name, project)
        }

    /**
     * Allows direct creation of a named tenant with configuration
     */
    fun create(
        name: String,
        configuration: Action<ViaductTenant>
    ): ViaductTenant {
        val tenant = tenantContainer.create(name)
        configuration.execute(tenant)
        return tenant
    }
}
