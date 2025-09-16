package viaduct.tenant.runtime.context.factory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class FactoryTest {
    @Test
    fun `const`() {
        assertEquals(1, Factory.const(1).mk(MockArgs()))
    }
}
