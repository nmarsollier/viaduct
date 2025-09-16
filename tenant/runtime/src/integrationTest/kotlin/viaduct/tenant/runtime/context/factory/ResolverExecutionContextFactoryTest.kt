package viaduct.tenant.runtime.context.factory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import viaduct.api.internal.InternalContext

@ExperimentalCoroutinesApi
class ResolverExecutionContextFactoryTest {
    @Test
    fun default() {
        val args = MockArgs()
        val ctx = ResolverExecutionContextFactory.default
            .mk(args.getExecutionContextArgs())

        val internal = ctx as InternalContext
        assertEquals(args.schema, internal.schema)
        assertEquals(args.reflectionLoader, internal.reflectionLoader)
    }
}
