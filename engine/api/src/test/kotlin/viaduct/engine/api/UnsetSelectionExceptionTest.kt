package viaduct.engine.api

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import viaduct.engine.api.mocks.MockSchema

class UnsetSelectionExceptionTest {
    private val obj = MockSchema.mk("type Query { x: Int }").queryType

    @Test
    fun `message -- field`() {
        assertTrue(
            UnsetSelectionException("x", obj).message.contains("field Query.x")
        )
    }

    @Test
    fun `message -- selection`() {
        assertTrue(
            UnsetSelectionException("y", obj).message.contains("aliased field y")
        )
    }

    @Test
    fun `message -- details`() {
        assertTrue(
            UnsetSelectionException("x", obj, "DETAILS").message.contains("DETAILS")
        )
    }
}
