package viaduct.service.api

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

internal class ExecutionInputTest {
    @Test
    fun testExecutionInput() {
        val executionInput = ExecutionInput("query", "scope123", mapOf("userId" to 1))
        assertNotNull(executionInput.variables)
        assertEquals("scope123", executionInput.schemaId)
    }
}
