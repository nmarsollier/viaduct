package viaduct.service.api.mocks

import viaduct.engine.api.TenantAPIBootstrapper
import viaduct.service.bootapi.TenantAPIBootstrapperActualBuilder

object MockTenantAPIBootstrapperBuilder {
    operator fun invoke(bootstrapper: TenantAPIBootstrapper) =
        object : TenantAPIBootstrapperActualBuilder {
            override fun create() = bootstrapper
        }
}
