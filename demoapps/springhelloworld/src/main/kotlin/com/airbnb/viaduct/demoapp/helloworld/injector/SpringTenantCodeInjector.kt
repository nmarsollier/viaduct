package com.airbnb.viaduct.demoapp.helloworld.injector

import javax.inject.Provider
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import viaduct.api.TenantCodeInjector

@Service
class SpringTenantCodeInjector(
    private val context: ApplicationContext
) : TenantCodeInjector {
    override fun <T> getProvider(clazz: Class<T>): Provider<T> {
        return Provider {
            context.getBean(clazz)
        }
    }
}
