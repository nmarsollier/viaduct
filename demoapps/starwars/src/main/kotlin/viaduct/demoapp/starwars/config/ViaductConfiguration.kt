package viaduct.demoapp.starwars.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import viaduct.service.api.Viaduct
import viaduct.service.runtime.SchemaRegistryBuilder
import viaduct.service.runtime.StandardViaduct
import viaduct.tenant.runtime.bootstrap.ViaductTenantAPIBootstrapper

const val SCHEMA_ID = "publicSchema"
const val SCOPE_ID = "publicScope"

@Configuration
class ViaductConfiguration {
    @Autowired
    lateinit var codeInjector: SpringTenantCodeInjector

    @Bean
    fun viaductService(): Viaduct =
        StandardViaduct.Builder()
            .withTenantAPIBootstrapperBuilder(
                ViaductTenantAPIBootstrapper.Builder()
                    .tenantCodeInjector(codeInjector)
                    .tenantPackagePrefix("viaduct.demoapp.starwars")
            )
            .withSchemaRegistryBuilder(
                SchemaRegistryBuilder()
                    .withFullSchemaFromResources("viaduct.demoapp.starwars", ".*\\.graphqls")
                    .registerScopedSchema(SCHEMA_ID, setOf(SCOPE_ID))
            ).build()
}
