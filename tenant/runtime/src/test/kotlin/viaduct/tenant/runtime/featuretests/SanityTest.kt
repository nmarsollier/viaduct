package viaduct.tenant.runtime.featuretests

import graphql.ExecutionResultImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import viaduct.api.FieldValue
import viaduct.api.context.FieldExecutionContext
import viaduct.api.context.MutationFieldExecutionContext
import viaduct.api.context.NodeExecutionContext
import viaduct.api.internal.InternalContext
import viaduct.api.types.Arguments
import viaduct.api.types.CompositeOutput
import viaduct.api.types.Object
import viaduct.tenant.runtime.context.NodeExecutionContextImpl
import viaduct.tenant.runtime.featuretests.fixtures.ArgumentsStub
import viaduct.tenant.runtime.featuretests.fixtures.Baz
import viaduct.tenant.runtime.featuretests.fixtures.FeatureTestBuilder
import viaduct.tenant.runtime.featuretests.fixtures.Foo
import viaduct.tenant.runtime.featuretests.fixtures.ObjectStub
import viaduct.tenant.runtime.featuretests.fixtures.Query
import viaduct.tenant.runtime.featuretests.fixtures.QueryStub
import viaduct.tenant.runtime.featuretests.fixtures.Query_HasArgs1_Arguments
import viaduct.tenant.runtime.featuretests.fixtures.UntypedFieldContext
import viaduct.tenant.runtime.featuretests.fixtures.assertJson

/** tests to ensure that [FeatureTest] behaves as expected */
@ExperimentalCoroutinesApi
@Suppress("USELESS_IS_CHECK")
class SanityTest {
    @Test
    fun `resolver uses an implicit UntypedFieldContext`() =
        FeatureTestBuilder()
            .sdl("type Query { x: Int }")
            .resolver("Query" to "x") { 42 }
            .build()
            .assertJson("{data: {x: 42}}", "{x}")

    @Test
    fun `resolver uses an explicit UntypedFieldContext`() =
        FeatureTestBuilder()
            .sdl("type Query { x: Int }")
            .resolver("Query" to "x") { _: UntypedFieldContext -> 42 }
            .build()
            .assertJson("{data: {x: 42}}", "{x}")

    @Test
    fun `resolver uses an explicit FieldExecutionContext`() =
        FeatureTestBuilder()
            .sdl("type Query { x: Int }")
            .resolver("Query" to "x") { _: FieldExecutionContext<*, *, *, *> -> 42 }
            .build()
            .assertJson("{data: {x: 42}}", "{x}")

    @Test
    fun `resolver uses an explicit FieldExecutionContext wrapper`() {
        class Context(
            inner: FieldExecutionContext<Object, Query, Arguments, CompositeOutput>
        ) : FieldExecutionContext<Object, Query, Arguments, CompositeOutput> by inner {
            val value: Int = 42
        }

        FeatureTestBuilder()
            .sdl("type Query { x: Int }")
            .resolver<Context, Object, Query, Arguments, CompositeOutput>(
                "Query" to "x",
                { ctx: Context -> ctx.value }
            )
            .build()
            .assertJson("{data: {x: 42}}", "{x}")
    }

    @Test
    fun `resolver uses an implicit UntypedMutationFieldContext`() =
        FeatureTestBuilder()
            .sdl(
                """
                    type Query { empty: Int }
                    type Mutation { x: Int }
                """.trimIndent()
            )
            .mutation("Mutation" to "x") { ctx ->
                assertTrue(ctx is MutationFieldExecutionContext<*, *, *, *>)
                42
            }
            .build()
            .assertJson("{data: {x: 42}}", "mutation {x}")

    @Test
    fun `resolver uses an explicit MutationFieldExecutionContext`() =
        FeatureTestBuilder()
            .sdl(
                """
                    type Query { empty: Int }
                    type Mutation { x: Int }
                """.trimIndent()
            )
            .resolver(
                "Mutation" to "x",
                { ctx: MutationFieldExecutionContext<Object, Query, Arguments, CompositeOutput> ->
                    assertTrue(ctx is MutationFieldExecutionContext<*, *, *, *>)
                    42
                }
            )
            .build()
            .assertJson("{data: {x: 42}}", "mutation {x}")

    @Test
    fun `resolver uses an explicit MutationFieldExecutionContext wrapper`() {
        class Context(
            inner: MutationFieldExecutionContext<Object, Query, Arguments, CompositeOutput>
        ) : MutationFieldExecutionContext<Object, Query, Arguments, CompositeOutput> by inner {
            val value: Int = 42
        }
        FeatureTestBuilder()
            .sdl(
                """
                    type Query { empty: Int }
                    type Mutation { x: Int }
                """.trimIndent()
            )
            .resolver(
                "Mutation" to "x",
                { ctx: Context ->
                    assertTrue(ctx is MutationFieldExecutionContext<*, *, *, *>)
                    42
                }
            )
            .build()
            .assertJson("{data: {x: 42}}", "mutation {x}")
    }

    @Test
    fun `resolver accesses parent object via explicit grt`() =
        FeatureTestBuilder()
            .sdl("type Query { x: Int }")
            .resolver(
                "Query" to "x",
                { ctx: FieldExecutionContext<Query, Query, Arguments, CompositeOutput.NotComposite> ->
                    assertTrue(ctx.objectValue is Query)
                    42
                }
            )
            .build()
            .assertJson("{data: {x: 42}}", "{x}")

    @Test
    fun `resolver accesses parent object via explicit ObjectStub`() =
        FeatureTestBuilder()
            .sdl("type Query { x: Int }")
            .resolver(
                "Query" to "x",
                { ctx: FieldExecutionContext<ObjectStub, QueryStub, Arguments, CompositeOutput.NotComposite> ->
                    assertTrue(ctx.objectValue is ObjectStub)
                    42
                }
            )
            .build()
            .assertJson("{data: {x: 42}}", "{x}")

    @Test
    fun `resolver accesses parent object via implicit ObjectStub`() =
        FeatureTestBuilder()
            .sdl("type Query { x: Int }")
            .resolver("Query" to "x") { ctx ->
                assertTrue(ctx.objectValue is ObjectStub)
                42
            }
            .build()
            .assertJson("{data: {x: 42}}", "{x}")

    @Test
    fun `resolver accesses arguments via explicit grt`() =
        FeatureTestBuilder()
            .sdl("type Query { hasArgs1(x: Int!): Int! }")
            .resolver(
                "Query" to "hasArgs1",
                { ctx: FieldExecutionContext<Query, Query, Query_HasArgs1_Arguments, CompositeOutput> ->
                    ctx.arguments.x
                }
            )
            .build()
            .assertJson("{data: {hasArgs1: 42}}", "{hasArgs1(x: 42)}")

    @Test
    fun `resolver accesses arguments via explicit ArgumentsStub`() =
        FeatureTestBuilder()
            .sdl("type Query { hasArgs1(x: Int!): Int! }")
            .resolver(
                "Query" to "hasArgs1",
                { ctx: FieldExecutionContext<Query, Query, ArgumentsStub, CompositeOutput> ->
                    ctx.arguments.get<Int>("x")
                }
            )
            .build()
            .assertJson("{data: {hasArgs1: 42}}", "{hasArgs1(x: 42)}")

    @Test
    fun `resolver accesses arguments via implicit ArgumentsStub`() =
        FeatureTestBuilder()
            .sdl("type Query { hasArgs1(x: Int!): Int! }")
            .resolver("Query" to "hasArgs1") { it.arguments.get<Int>("x") }
            .build()
            .assertJson("{data: {hasArgs1: 42}}", "{hasArgs1(x: 42)}")

    @Test
    fun `resolver accesses selections via explicit grt`() {
        FeatureTestBuilder()
            .sdl(
                """
                    type Query { foo: Foo }
                    type Foo { value: String }
                """.trimIndent()
            )
            .resolver(
                "Query" to "foo",
                { ctx: FieldExecutionContext<Query, Query, Arguments, Foo> ->
                    assertTrue(ctx.selections().contains(Foo.Reflection.Fields.value))
                    null
                }
            )
            .build()
            .assertJson("{data: {foo: null}}", "{foo {value}}")
    }

    @Test
    fun `nodeResolver uses an implicit UntypedNodeContext`() =
        FeatureTestBuilder()
            .sdl(
                """
                    interface Node { id: ID! }
                    type Baz { id: ID! }
                    type Query { baz: Baz }
                """.trimIndent()
            )
            .grtPackage(Query.Reflection)
            .resolver("Query" to "baz") { it.nodeFor(it.globalIDFor(Baz.Reflection, "")) }
            .nodeResolver("Baz") { ctx ->
                Baz.Builder(ctx).build()
            }
            .build()
            .assertJson("{data: {baz: {__typename: \"Baz\"}}}", "{baz {__typename}}")

    @Test
    fun `nodeResolver uses an explicit NodeExecutionContext`() =
        FeatureTestBuilder()
            .sdl(
                """
                    interface Node { id: ID! }
                    type Baz { id: ID! }
                    type Query { baz: Baz }
                """.trimIndent()
            )
            .grtPackage(Query.Reflection)
            .resolver("Query" to "baz") { it.nodeFor(it.globalIDFor(Baz.Reflection, "")) }
            .nodeResolver("Baz") { ctx: NodeExecutionContext<Baz> ->
                Baz.Builder(ctx).build()
            }
            .build()
            .assertJson("{data: {baz: {__typename: \"Baz\"}}}", "{baz {__typename}}")

    @Test
    fun `nodeResolver uses an explicit wrapped NodeExecutionContext`() {
        class Context(inner: NodeExecutionContextImpl<Baz>) : NodeExecutionContext<Baz> by inner, InternalContext by inner

        FeatureTestBuilder()
            .sdl(
                """
                    interface Node { id: ID! }
                    type Baz { id: ID! }
                    type Query { baz: Baz }
                """.trimIndent()
            )
            .grtPackage(Query.Reflection)
            .resolver("Query" to "baz") { it.nodeFor(it.globalIDFor(Baz.Reflection, "")) }
            .nodeResolver("Baz") { ctx: Context ->
                Baz.Builder(ctx).build()
            }
            .build()
            .assertJson("{data: {baz: {__typename: \"Baz\"}}}", "{baz {__typename}}")
    }

    @Test
    fun `nodeBatchResolver uses an implicit UntypedNodeContext`() =
        FeatureTestBuilder()
            .sdl(
                """
                    interface Node { id: ID! }
                    type Baz { id: ID! }
                    type Query { baz: Baz }
                """.trimIndent()
            )
            .grtPackage(Query.Reflection)
            .resolver("Query" to "baz") { it.nodeFor(it.globalIDFor(Baz.Reflection, "")) }
            .nodeBatchResolver("Baz") { ctxs ->
                ctxs.map { ctx -> FieldValue.ofValue(Baz.Builder(ctx).build()) }
            }
            .build()
            .execute("{baz {__typename}}")
            .assertJson("{data: {baz: {__typename: \"Baz\"}}}")

    @Test
    fun `nodeBatchResolver uses an explicit NodeExecutionContext`() =
        FeatureTestBuilder()
            .sdl(
                """
                    interface Node { id: ID! }
                    type Baz { id: ID! }
                    type Query { baz: Baz }
                """.trimIndent()
            )
            .grtPackage(Query.Reflection)
            .resolver("Query" to "baz") { it.nodeFor(it.globalIDFor(Baz.Reflection, "")) }
            .nodeBatchResolver("Baz") { ctxs: List<NodeExecutionContext<Baz>> ->
                ctxs.map { ctx -> FieldValue.ofValue(Baz.Builder(ctx).build()) }
            }
            .build()
            .execute("{baz {__typename}}")
            .assertJson("{data: {baz: {__typename: \"Baz\"}}}")

    @Test
    fun `nodeBatchResolver uses an explicit wrapped NodeExecutionContext`() {
        class Context(inner: NodeExecutionContextImpl<Baz>) : NodeExecutionContext<Baz> by inner, InternalContext by inner

        FeatureTestBuilder()
            .sdl(
                """
                    interface Node { id: ID! }
                    type Baz { id: ID! }
                    type Query { baz: Baz }
                """.trimIndent()
            )
            .grtPackage(Query.Reflection)
            .resolver("Query" to "baz") { it.nodeFor(it.globalIDFor(Baz.Reflection, "")) }
            .nodeBatchResolver("Baz") { ctxs: List<Context> ->
                ctxs.map { ctx -> FieldValue.ofValue(Baz.Builder(ctx).build()) }
            }
            .build()
            .execute("{baz {__typename}}")
            .assertJson("{data: {baz: {__typename: \"Baz\"}}}")
    }

    @Test
    fun `ExecutionResult_assertJson -- unparseable`() {
        val result = ExecutionResultImpl(emptyMap<String, Any?>(), emptyList())
        assertThrows<IllegalArgumentException> {
            result.assertJson("{")
        }
    }

    @Test
    fun `ExecutionResult_assertJson -- does not match`() {
        val result = ExecutionResultImpl(emptyMap<String, Any?>(), emptyList())
        val err = runCatching {
            result.assertJson("""{"x": 42}""")
        }.exceptionOrNull()
        assertEquals("org.opentest4j.AssertionFailedError", err?.javaClass?.name)
    }

    @Test
    fun `ExecutionResult_assertJson -- match`() {
        val result = ExecutionResultImpl(mapOf("x" to 42), emptyList())
        assertDoesNotThrow {
            result.assertJson("""{"data": {"x": 42}}""")
        }
    }

    @Test
    fun `ExecutionResult_assertJson -- quality-of-life extras`() {
        val result = ExecutionResultImpl(mapOf("x" to 42), emptyList())
        // single-quoted keys
        result.assertJson("""{'data': {'x': 42}}""")

        // unquoted keys
        result.assertJson("""{data: {x: 42}}""")

        // with block comments
        result.assertJson(
            """
                {
                  /* test comment */
                  "data": {
                      "x": 42
                  }
                }
            """.trimIndent()
        )

        // with line comments
        result.assertJson(
            """
                {
                  // test comment
                  "data": {
                      "x": 42
                  }
                }
            """.trimIndent()
        )

        // trailing comma
        result.assertJson("""{"data": {"x": 42,},}""")
    }
}
