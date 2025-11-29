package viaduct.service.api.spi

import viaduct.utils.api.InternalApi

/**
 * Represents a feature flag with a name
 */
@InternalApi
interface Flag {
    val flagName: String
}
