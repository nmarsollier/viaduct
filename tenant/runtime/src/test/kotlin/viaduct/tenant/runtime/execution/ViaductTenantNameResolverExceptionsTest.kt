package viaduct.tenant.runtime.execution

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ViaductTenantNameResolverExceptionsTest {
    @Test
    fun testResolversCallChain() {
        val exception = ViaductTenantResolverException(RuntimeException("foo"), "Pet.name")
        assertEquals("Pet.name", exception.resolversCallChain)

        val outerException = ViaductTenantResolverException(exception, "Person.pet")
        assertEquals("Person.pet > Pet.name", outerException.resolversCallChain)
    }
}
