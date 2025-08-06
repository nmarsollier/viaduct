package viaduct.engine.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FieldCheckerDispatcherRegistryTest {
    @Test
    fun `Empty`() {
        val reg = FieldCheckerDispatcherRegistry.Empty
        assertEquals(null, reg.getCheckerExecutor("Query", "__typename"))
        assertEquals(null, reg.getCheckerExecutor("Foo", "foo"))
        assertEquals(null, reg.getCheckerExecutor("", ""))
    }
}
