package viaduct.service.bootapi

import viaduct.engine.api.TenantAPIBootstrapper
import viaduct.service.api.spi.TenantAPIBootstrapperBuilder

/**
 * An interface for builders of TenantAPIBootstrapper implementations.
 *
 * This is the internal implementation interface that all TenantAPIBootstrapperBuilder
 * implementations must implement. It defines the actual contract between StandardViaduct.Builder
 * and the TenantAPIBootstrapper implementation builders.
 */
interface TenantAPIBootstrapperActualBuilder : TenantAPIBootstrapperBuilder {
    /**
     * Creates a TenantAPIBootstrapper instance
     *
     * @return A TenantAPIBootstrapper implementation
     */
    fun create(): TenantAPIBootstrapper
}
