package viaduct.tenant.runtime.bootstrap

import com.google.common.annotations.VisibleForTesting
import viaduct.utils.slf4j.logger

/**
 * Factory for creating ViaductTenantResolverClassFinder instances.
 *
 * This factory implements the TenantResolverClassFinderFactory interface to provide
 * a standardized way of creating tenant resolver class finders configured with
 * specific package names for class discovery.
 *
 * @see ViaductTenantResolverClassFinder
 * @see TenantResolverClassFinderFactory
 */
class ViaductTenantResolverClassFinderFactory
    @VisibleForTesting
    internal constructor(private val grtPackagePrefix: String) : TenantResolverClassFinderFactory {
        constructor() : this(grtPackagePrefix = "viaduct.api.grts")

        companion object {
            private val log by logger()
        }

        override fun create(packageName: String): TenantResolverClassFinder {
            log.info("Discovering resolvers for TenantModule [$packageName]")
            return ViaductTenantResolverClassFinder(packageName, grtPackagePrefix)
        }
    }
