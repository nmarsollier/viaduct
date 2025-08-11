package viaduct.engine.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FieldCheckerDispatcherRegistryTest {
    @Test
    fun `Empty`() {
        val reg = FieldCheckerDispatcherRegistry.Empty
        assertEquals(null, reg.getFieldCheckerDispatcher("Query", "__typename"))
        assertEquals(null, reg.getFieldCheckerDispatcher("Foo", "foo"))
        assertEquals(null, reg.getFieldCheckerDispatcher("", ""))
    }
}
