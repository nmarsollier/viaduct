package viaduct.engine.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TypeCheckerDispatcherRegistryTest {
    @Test
    fun `Empty`() {
        val reg = TypeCheckerDispatcherRegistry.Empty
        assertEquals(null, reg.getTypeCheckerDispatcher("Query"))
        assertEquals(null, reg.getTypeCheckerDispatcher("Foo"))
        assertEquals(null, reg.getTypeCheckerDispatcher(""))
    }
}
