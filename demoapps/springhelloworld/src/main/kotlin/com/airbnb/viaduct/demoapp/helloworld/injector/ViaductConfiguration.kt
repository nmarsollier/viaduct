package com.airbnb.viaduct.demoapp.helloworld.injector

import com.airbnb.viaduct.demoapp.helloworld.rest.SCHEMA_ID
import com.airbnb.viaduct.demoapp.helloworld.rest.SCOPE_ID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import viaduct.service.ViaductBuilder
import viaduct.service.runtime.ViaductSchemaRegistryBuilder
import viaduct.tenant.runtime.bootstrap.ViaductTenantAPIBootstrapper

@Configuration
class ViaductConfiguration {
    @Autowired
    lateinit var codeInjector: SpringTenantCodeInjector

    @Bean
    fun viaductService() =
        ViaductBuilder()
            .withTenantAPIBootstrapperBuilder(
                ViaductTenantAPIBootstrapper.Builder()
                    .tenantCodeInjector(codeInjector)
                    .tenantPackagePrefix("viaduct.demoapp.tenant1")
            )
            .withSchemaRegistryBuilder(
                ViaductSchemaRegistryBuilder()
                    .withFullSchemaFromResources("viaduct.demoapp", ".*demoapp.*graphqls")
                    .registerScopedSchema(SCHEMA_ID, setOf(SCOPE_ID))
            ).build()
}
