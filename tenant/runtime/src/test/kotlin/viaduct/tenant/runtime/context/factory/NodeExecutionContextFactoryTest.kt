package viaduct.tenant.runtime.context.factory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import viaduct.api.mocks.MockGlobalID
import viaduct.api.select.SelectionSet

@ExperimentalCoroutinesApi
class NodeExecutionContextFactoryTest {
    @Test
    fun create() {
        val type = Baz.Reflection
        val globalID = MockGlobalID(type, "-1")
        val args = MockArgs(globalID = globalID.toString())

        val selections = SelectionSet.empty(type)

        val ctx = NodeExecutionContextMetaFactory
            .create(Factory.const(selections))
            .make(args.getNodeArgs())

        assertEquals(globalID, ctx.id)
        assertEquals(selections, ctx.selections())
    }
}
