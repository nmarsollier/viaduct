package viaduct.tenant.runtime.internal

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.api.ViaductFrameworkException
import viaduct.api.context.ExecutionContext
import viaduct.api.globalid.GlobalID
import viaduct.api.globalid.GlobalIDCodec
import viaduct.api.internal.InternalContext
import viaduct.api.internal.ReflectionLoader
import viaduct.api.internal.internal
import viaduct.api.mocks.MockGlobalIDCodec
import viaduct.api.mocks.MockReflectionLoader
import viaduct.api.reflect.Type
import viaduct.api.select.SelectionSet
import viaduct.api.types.CompositeOutput
import viaduct.api.types.NodeObject
import viaduct.api.types.Object
import viaduct.api.types.Query
import viaduct.engine.api.ViaductSchema
import viaduct.engine.api.mocks.MockSchema

class InternalContextImplTest {
    private val schema = MockSchema.minimal

    @Test
    fun simple() {
        val ctx = InternalContextImpl(schema, MockGlobalIDCodec(), MockReflectionLoader())
        assertSame(schema, ctx.schema)
    }

    @Test
    fun executionContextInternal() {
        val ec = TestCompositeContext()
        assertSame(ec, ec.internal)
    }

    @Test
    fun `ExecutionContext_internal -- not an InternalContext`() {
        val ec = TestExecutionContext()
        assertThrows<ViaductFrameworkException> {
            ec.internal
        }
    }
}

private open class TestExecutionContext : ExecutionContext {
    override fun <T : CompositeOutput> selectionsFor(
        type: Type<T>,
        selections: String,
        variables: Map<String, Any?>
    ): SelectionSet<T> = TODO()

    override suspend fun <T : Query> query(selections: SelectionSet<T>): T = TODO()

    override fun <T : NodeObject> nodeFor(id: GlobalID<T>): T = TODO()

    override fun <T : Object> globalIDFor(
        type: Type<T>,
        internalID: String
    ): GlobalID<T> = TODO()

    override fun <T : Object> globalIDStringFor(
        type: Type<T>,
        internalID: String
    ) = TODO()
}

private open class TestCompositeContext : TestExecutionContext(), InternalContext {
    override val schema: ViaductSchema get() = TODO()
    override val globalIDCodec: GlobalIDCodec get() = TODO()
    override val reflectionLoader: ReflectionLoader get() = TODO()

    override fun <T : Object> globalIDStringFor(
        type: Type<T>,
        internalID: String
    ) = TODO()
}
