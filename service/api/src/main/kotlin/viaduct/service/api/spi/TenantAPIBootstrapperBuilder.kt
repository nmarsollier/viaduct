package viaduct.service.api.spi

/**
 * A tagging interface for builders of TenantAPIBootstrapper implementations.
 *
 * This interface is used by StandardViaduct.Builder to accept builder instances
 * for creating TenantAPIBootstrapper implementations. As noted in StandardViaduct,
 * these instances must come from valid Tenant API implementations, which understand
 * a special protocol expected by StandardViadcut.
 */
interface TenantAPIBootstrapperBuilder
