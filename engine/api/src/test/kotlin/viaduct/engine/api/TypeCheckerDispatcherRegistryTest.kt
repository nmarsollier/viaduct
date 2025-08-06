package viaduct.engine.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TypeCheckerDispatcherRegistryTest {
    @Test
    fun `Empty`() {
        val reg = TypeCheckerDispatcherRegistry.Empty
        assertEquals(null, reg.getTypeCheckerExecutor("Query"))
        assertEquals(null, reg.getTypeCheckerExecutor("Foo"))
        assertEquals(null, reg.getTypeCheckerExecutor(""))
    }
}
