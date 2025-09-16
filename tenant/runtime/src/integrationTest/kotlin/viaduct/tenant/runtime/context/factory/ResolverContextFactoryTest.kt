package viaduct.tenant.runtime.context.factory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.api.context.FieldExecutionContext
import viaduct.api.types.Arguments
import viaduct.api.types.CompositeOutput

private typealias FieldContextType = FieldExecutionContext<Query, Query, Arguments.NoArguments, CompositeOutput.NotComposite>

@ExperimentalCoroutinesApi
class ResolverContextFactoryTest {
    private val args = MockArgs().getFieldArgs()
    private val fieldContextFactory = FieldExecutionContextMetaFactory.create(
        ObjectFactory.forClass(Query::class),
        ObjectFactory.forClass(Query::class),
        ArgumentsFactory.NoArguments,
        SelectionSetFactory.NoSelections
    )

    class Context(inner: FieldContextType) : FieldContextType by inner

    @Test
    fun `forClass -- not Context`() {
        assertThrows<IllegalArgumentException> {
            ResolverContextFactory.forClass(FieldExecutionContext::class, fieldContextFactory)
        }
    }

    @Test
    fun `forClass -- is Context`() {
        val ctx = ResolverContextFactory
            .forClass(Context::class, fieldContextFactory)
            .make(args)
        assertTrue(ctx is Context)
    }

    @Test
    fun `ifContext -- not Context`() {
        val factory = ResolverContextFactory.ifContext(FieldExecutionContext::class, fieldContextFactory)
        assertEquals(fieldContextFactory, factory)
    }

    @Test
    fun `ifContext -- is Context`() {
        val ctx = ResolverContextFactory
            .ifContext(Context::class, fieldContextFactory)
            .make(args)
        assertTrue(ctx is Context)
    }
}
