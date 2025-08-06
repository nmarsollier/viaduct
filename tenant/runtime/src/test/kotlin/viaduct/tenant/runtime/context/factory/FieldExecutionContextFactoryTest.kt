package viaduct.tenant.runtime.context.factory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import viaduct.api.mocks.MockExecutionContext
import viaduct.api.select.SelectionSet
import viaduct.api.types.Arguments

@ExperimentalCoroutinesApi
class FieldExecutionContextFactoryTest {
    @Test
    fun create() {
        MockArgs().let { args ->
            val ec = MockExecutionContext(args.internalContext)

            val obj = Foo.Builder(ec).build()
            val q = Query.Builder(ec).build()
            val arguments = Arguments.NoArguments
            val selectionSet = SelectionSet.NoSelections

            val factory = FieldExecutionContextMetaFactory.create(
                objectValue = Factory.const(obj),
                queryValue = Factory.const(q),
                arguments = Factory.const(arguments),
                selectionSet = Factory.const(selectionSet),
            )

            val ctx = factory.make(args.getFieldArgs())
            assertTrue(ctx.objectValue === obj)
            assertTrue(ctx.queryValue === q)
            assertTrue(ctx.arguments === arguments)
            assertTrue(ctx.selections() === selectionSet)
        }
    }
}
