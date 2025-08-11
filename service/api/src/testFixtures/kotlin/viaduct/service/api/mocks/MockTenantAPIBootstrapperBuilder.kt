package viaduct.service.api.mocks

import viaduct.engine.api.TenantAPIBootstrapper
import viaduct.service.api.spi.TenantAPIBootstrapperBuilder

object MockTenantAPIBootstrapperBuilder {
    operator fun invoke(bootstrapper: TenantAPIBootstrapper) =
        object : TenantAPIBootstrapperBuilder {
            override fun create() = bootstrapper
        }
}
