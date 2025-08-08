package viaduct.service.api.spi

import graphql.language.OperationDefinition.Operation

enum class Flags(override val flagName: String) : Flag {
    USE_MODERN_EXECUTION_STRATEGY_QUERY("use_modern_execution_strategy_query"),
    USE_MODERN_EXECUTION_STRATEGY_MUTATION("use_modern_execution_strategy_mutation"),
    USE_MODERN_EXECUTION_STRATEGY_SUBSCRIPTION("use_modern_execution_strategy_subscription"),
    USE_MODERN_EXECUTION_STRATEGY_FOR_MODERN_FIELDS("use_modern_execution_strategy_for_modern_fields"),

    EXECUTE_ACCESS_CHECKS_IN_MODERN_EXECUTION_STRATEGY("execute_access_checks_in_modern_execution_strategy"),

    DISABLE_QUERY_PLAN_CACHE("disable_query_plan_cache"),
    ;

    companion object {
        /**
         * return the [Flag] that determines if the modern execution strategy
         * is enabled for the provided [operationType]
         */
        fun useModernExecutionStrategy(operationType: Operation): Flag =
            when (operationType) {
                Operation.QUERY -> USE_MODERN_EXECUTION_STRATEGY_QUERY
                Operation.MUTATION -> USE_MODERN_EXECUTION_STRATEGY_MUTATION
                Operation.SUBSCRIPTION -> USE_MODERN_EXECUTION_STRATEGY_SUBSCRIPTION
            }

        /** the set of all flags that control modern execution strategy enablement */
        val useModernExecutionStrategyFlags: Set<Flag> = setOf(
            USE_MODERN_EXECUTION_STRATEGY_QUERY,
            USE_MODERN_EXECUTION_STRATEGY_MUTATION,
            USE_MODERN_EXECUTION_STRATEGY_SUBSCRIPTION,
            USE_MODERN_EXECUTION_STRATEGY_FOR_MODERN_FIELDS
        )
    }
}
