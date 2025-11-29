package viaduct.service.api.spi

import viaduct.engine.api.TenantAPIBootstrapper
import viaduct.utils.api.StableApi

/**
 * A tagging interface for builders of TenantAPIBootstrapper implementations.
 *
 * This interface is used by ViaductBuilder to accept builder instances
 * for creating TenantAPIBootstrapper implementations. As noted in StandardViaduct,
 * these instances must come from valid Tenant API implementations, which understand
 * a special protocol expected by StandardViadcut.
 */
@StableApi
interface TenantAPIBootstrapperBuilder {
    fun create(): TenantAPIBootstrapper
}
