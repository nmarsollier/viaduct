package com.airbnb.viaduct.demoapp.helloworld.injector

import com.airbnb.viaduct.demoapp.helloworld.rest.SCHEMA_ID
import com.airbnb.viaduct.demoapp.helloworld.rest.SCOPE_ID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import viaduct.service.runtime.SchemaRegistryBuilder
import viaduct.service.runtime.StandardViaduct
import viaduct.tenant.runtime.bootstrap.ViaductTenantAPIBootstrapper

@Configuration
class ViaductConfiguration {
    @Autowired
    lateinit var codeInjector: SpringTenantCodeInjector

    @Bean
    fun viaductService() =
        StandardViaduct.Builder()
            .withTenantAPIBootstrapperBuilder(
                ViaductTenantAPIBootstrapper.Builder()
                    .tenantCodeInjector(codeInjector)
                    .tenantPackagePrefix("viaduct.demoapp.tenant1")
            )
            .withSchemaRegistryBuilder(
                SchemaRegistryBuilder()
                    .withFullSchemaFromResources("viaduct.demoapp", ".*demoapp.*graphqls")
                    .registerScopedSchema(SCHEMA_ID, setOf(SCOPE_ID))
            ).build()
}
