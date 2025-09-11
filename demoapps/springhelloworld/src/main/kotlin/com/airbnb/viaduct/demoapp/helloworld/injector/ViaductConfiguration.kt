package com.airbnb.viaduct.demoapp.helloworld.injector

import com.airbnb.viaduct.demoapp.helloworld.rest.SCHEMA_ID
import com.airbnb.viaduct.demoapp.helloworld.rest.SCOPE_ID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import viaduct.service.BasicViaductFactory
import viaduct.service.SchemaRegistrationInfo
import viaduct.service.SchemaScopeInfo
import viaduct.service.TenantRegistrationInfo

@Configuration
class ViaductConfiguration {
    @Autowired
    lateinit var codeInjector: SpringTenantCodeInjector

    @Bean
    fun viaductService() =
        BasicViaductFactory.create(
            schemaRegistrationInfo = SchemaRegistrationInfo(
                scopes = listOf(SchemaScopeInfo(SCHEMA_ID, setOf(SCOPE_ID))),
                packagePrefix = "viaduct.demoapp",
                resourcesIncluded = ".*demoapp.*graphqls"
            ),
            tenantRegistrationInfo = TenantRegistrationInfo(
                tenantPackagePrefix = "viaduct.demoapp.tenant1",
                tenantCodeInjector = codeInjector
            )
        )
}
