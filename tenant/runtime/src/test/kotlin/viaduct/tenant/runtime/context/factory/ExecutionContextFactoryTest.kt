package viaduct.tenant.runtime.context.factory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import viaduct.api.internal.InternalContext

@ExperimentalCoroutinesApi
class ExecutionContextFactoryTest {
    @Test
    fun default() {
        val args = MockArgs()
        val ctx = ExecutionContextFactory.default
            .mk(args.getExecutionContextArgs())

        val internal = ctx as InternalContext
        assertEquals(args.schema.schema, internal.schema)
        assertEquals(args.reflectionLoader, internal.reflectionLoader)
    }
}
