package viaduct.service.api.spi

import graphql.language.OperationDefinition.Operation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlagsTest {
    @Test
    fun `useModernExecutionStrategy`() {
        assertEquals(
            mapOf(
                Operation.QUERY to Flags.USE_MODERN_EXECUTION_STRATEGY_QUERY,
                Operation.MUTATION to Flags.USE_MODERN_EXECUTION_STRATEGY_MUTATION,
                Operation.SUBSCRIPTION to Flags.USE_MODERN_EXECUTION_STRATEGY_SUBSCRIPTION
            ),
            Operation.values().associateBy({ it }, Flags::useModernExecutionStrategy)
        )
    }

    @Test
    fun `useModernExecutionStrategyFlags`() {
        listOf(
            Flags.USE_MODERN_EXECUTION_STRATEGY_QUERY,
            Flags.USE_MODERN_EXECUTION_STRATEGY_MUTATION,
            Flags.USE_MODERN_EXECUTION_STRATEGY_SUBSCRIPTION,
            Flags.USE_MODERN_EXECUTION_STRATEGY_FOR_MODERN_FIELDS
        ).forEach { flag ->
            assertTrue(Flags.useModernExecutionStrategyFlags.contains(flag))
        }
    }
}
