package viaduct.service.api.spi

import viaduct.service.api.spi.Flags.EXECUTE_ACCESS_CHECKS_IN_MODERN_EXECUTION_STRATEGY
import viaduct.service.api.spi.Flags.USE_MODERN_EXECUTION_STRATEGY_FOR_MODERN_FIELDS
import viaduct.service.api.spi.Flags.USE_MODERN_EXECUTION_STRATEGY_MUTATION
import viaduct.service.api.spi.Flags.USE_MODERN_EXECUTION_STRATEGY_QUERY
import viaduct.service.api.spi.Flags.USE_MODERN_EXECUTION_STRATEGY_SUBSCRIPTION

/**
 * Interface for managing feature flags.
 */
interface FlagManager {
    /**
     * Returns a boolean representing whether or not [flag] is enabled. Impl should execute very quickly as it could
     * be used in the hot path.
     */
    fun isEnabled(flag: Flag): Boolean

    companion object {
        object NoOpFlagManager : FlagManager {
            override fun isEnabled(flag: Flag): Boolean = false
        }

        object DefaultFlagManager : FlagManager {
            override fun isEnabled(flag: Flag): Boolean =
                when (flag) {
                    USE_MODERN_EXECUTION_STRATEGY_QUERY,
                    USE_MODERN_EXECUTION_STRATEGY_MUTATION,
                    USE_MODERN_EXECUTION_STRATEGY_SUBSCRIPTION,
                    USE_MODERN_EXECUTION_STRATEGY_FOR_MODERN_FIELDS,
                    EXECUTE_ACCESS_CHECKS_IN_MODERN_EXECUTION_STRATEGY -> true

                    else -> false
                }
        }
    }
}
