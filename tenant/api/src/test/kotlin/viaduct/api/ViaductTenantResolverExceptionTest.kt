package viaduct.api

import java.lang.reflect.InvocationTargetException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class ViaductTenantResolverExceptionTest {
    @Test
    fun getResolversCallChain() {
        val exception = ViaductTenantResolverException(
            cause = ViaductTenantResolverException(
                cause = ViaductTenantResolverException(
                    cause = RuntimeException(),
                    resolver = "ResolverC",
                ),
                resolver = "ResolverB",
            ),
            resolver = "ResolverA",
        )

        val callChain = exception.resolversCallChain
        assertEquals("ResolverA > ResolverB > ResolverC", callChain)
    }

    @Test
    fun testWrapFrameworkException() =
        runBlockingTest {
            assertThrows<ViaductFrameworkException> {
                wrapResolveException("ResolverA") {
                    throw ViaductFrameworkException("a framework exception occurred")
                }
            }
        }

    @Test
    fun testWrapTenantException() =
        runBlockingTest {
            assertThrows<ViaductTenantResolverException> {
                wrapResolveException("ResolverA") {
                    throw InvocationTargetException(RuntimeException("a tenant exception occurred"))
                }
            }
        }
}
