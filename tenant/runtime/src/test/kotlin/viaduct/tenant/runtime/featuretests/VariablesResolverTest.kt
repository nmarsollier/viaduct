package viaduct.tenant.runtime.featuretests

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.engine.api.FromArgumentVariable
import viaduct.engine.api.FromObjectFieldVariable
import viaduct.engine.api.FromQueryFieldVariable
import viaduct.engine.api.GraphQLBuildError
import viaduct.tenant.runtime.featuretests.fixtures.FeatureTestBuilder
import viaduct.tenant.runtime.featuretests.fixtures.UntypedFieldContext
import viaduct.tenant.runtime.featuretests.fixtures.const
import viaduct.tenant.runtime.internal.VariablesProviderInfo

@ExperimentalCoroutinesApi
class VariablesResolverTest {
    @Test
    fun `variables provider -- variable name overlaps with bound field arg`() =
        FeatureTestBuilder()
            .sdl("type Query { foo(x:Int): Int!, bar(x:Int!): Int! }")
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar") * 5 },
                "bar(x:\$x)",
                variables = listOf(FromArgumentVariable("x", "x")),
                variablesProvider = VariablesProviderInfo.const(mapOf("x" to 2)),
            )
            .resolver("Query" to "bar") { it.arguments.get<Int>("x") * 3 }
            .let {
                assertThrows<Exception> {
                    it.build()
                }
                Unit
            }

    @Test
    fun `variables provider -- uses an arg variable with the same name as an unbound argument`() =
        FeatureTestBuilder()
            .sdl("type Query { foo(x:Int):Int, bar(x:Int):Int, baz(x:Int):Int }")
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("baz") * 11 },
                "baz(x:\$x)",
                variablesProvider = VariablesProviderInfo.const(mapOf("x" to 2))
            )
            .resolver(
                "Query" to "bar",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("baz") * 7 },
                "baz(x:\$x)",
                variables = listOf(FromArgumentVariable("x", "x"))
            )
            .resolver("Query" to "baz") { it.arguments.get<Int>("x") * 5 }
            .build()
            .assertJson("{data:{foo:110, bar:105}}", "{foo(x:2) bar(x:3)}")

    @Test
    fun `from arg -- simple`() =
        FeatureTestBuilder()
            .sdl("type Query { foo(y: Int!): Int!, bar(x:Int!): Int! }")
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar") * 5 },
                "bar(x:\$y)",
                variables = listOf(
                    FromArgumentVariable("y", "y")
                )
            )
            .resolver("Query" to "bar") { it.arguments.get<Int>("x") * 3 }
            .build()
            .assertJson("{data: {foo: 30}}", "{foo(y:2)}")

    @Test
    fun `from arg -- binds argument to variable with a different name`() =
        FeatureTestBuilder()
            .sdl("type Query { foo(y: Int!): Int!, bar(x:Int!): Int! }")
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar") * 5 },
                "bar(x:\$vary)",
                variables = listOf(
                    FromArgumentVariable("vary", "y")
                )
            )
            .resolver("Query" to "bar") { it.arguments.get<Int>("x") * 3 }
            .build()
            .assertJson("{data: {foo: 30}}", "{foo(y:2)}")

    @Test
    fun `from arg -- path`() =
        FeatureTestBuilder()
            .sdl(
                """
                    input Inp { x:Int! }
                    type Query { foo(inp:Inp!): Int!, bar(x:Int!):Int! }
                """.trimIndent()
            )
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar") * 5 },
                "bar(x:\$x)",
                variables = listOf(FromArgumentVariable("x", "inp.x"))
            )
            .resolver("Query" to "bar") { it.arguments.get<Int>("x") * 3 }
            .build()
            .assertJson("{data: {foo: 30}}", "{ foo(inp:{x:2}) }")

    @Test
    fun `from arg -- empty path`() =
        FeatureTestBuilder()
            .sdl(
                """
                    input Inp { x:Int }
                    type Query { foo(x:Int!): Int!, bar(inp:Inp!):Int! }
                """.trimIndent()
            )
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar") * 5 },
                "bar(x:\$x)",
                variables = listOf(FromArgumentVariable("inp", ""))
            )
            .resolver("Query" to "bar") { it.arguments.get<Int>("x") * 3 }
            .let {
                assertThrows<Exception> {
                    it.build()
                }
                Unit
            }

    @Test
    fun `from arg -- arg has default value`() =
        FeatureTestBuilder()
            .sdl("type Query { foo(x:Int!=2):Int!, bar(x:Int):Int }")
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar") * 5 },
                "bar(x:\$x)",
                variables = listOf(FromArgumentVariable("x", "x"))
            )
            .resolver("Query" to "bar") { it.arguments.get<Int>("x") * 3 }
            .build()
            .let {
                // querying without an argument value will use the argument default
                it.assertJson("{data: {foo: 30}}", "{foo}")

                // querying with an argument value will use the provided value
                it.assertJson("{data: {foo: 45}}", "{foo(x:3)}")
            }

    @Test
    fun `from arg -- input field has default value`() =
        FeatureTestBuilder()
            .sdl(
                """
                    input Inp { x:Int!=2 }
                    type Query { foo(inp:Inp!):Int!, bar(x:Int!):Int! }
                """.trimIndent()
            )
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar") * 5 },
                "bar(x:\$x)",
                variables = listOf(FromArgumentVariable("x", "inp.x"))
            )
            .resolver("Query" to "bar") { it.arguments.get<Int>("x") * 3 }
            .build()
            .let {
                // querying with an empty input object will use the field default
                it.assertJson("{data: {foo: 30}}", "{foo(inp:{})}")

                // querying with a populated object value will use the provided value
                it.assertJson("{data: {foo: 45}}", "{foo(inp:{x:3})}")
            }

    @Test
    fun `from arg -- path traverses through null object`() =
        FeatureTestBuilder()
            .sdl(
                """
                    input Inp { x:Int!=2 }
                    type Query { foo(inp:Inp):Int, bar(x:Int):Int }
                """.trimIndent()
            )
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext ->
                    ctx.objectValue.get<Int?>("bar")
                        ?.let { it * 5 }
                        ?: 7
                },
                "bar(x:\$x)",
                variables = listOf(FromArgumentVariable("x", "inp.x"))
            )
            .resolver("Query" to "bar") {
                it.arguments.tryGet<Int>("x")?.let { arg -> arg * 3 }
            }
            .build()
            // querying with null inp will fail traversal into Inp.x
            .assertJson("{data: {foo: 7}}", "{foo(inp:null)}")

    @Test
    fun `from arg -- variable name overlaps with bound selection arg`() =
        FeatureTestBuilder()
            .sdl("type Query { foo(x:Int):Int!, bar(x:Int!):Int!, baz:Int! }")
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar") * 5 },
                "bar(x:\$x), baz",
                variables = listOf(
                    FromArgumentVariable("x", "x"),
                    FromObjectFieldVariable("x", "baz"),
                ),
            )
            .resolver("Query" to "bar") { it.arguments.get<Int>("x") * 3 }
            .let {
                assertThrows<Exception> {
                    it.build()
                }
                Unit
            }

    @Test
    fun `from arg -- type validation`() =
        // this test case tries to use a nullable value, Query.foo(x:), in a position
        // where a non-nullable one is required, Query.bar(x:)
        FeatureTestBuilder()
            .sdl("type Query { foo(x:Int):Int!, bar(x:Int!):Int! }")
            .resolver(
                "Query" to "foo",
                { _: UntypedFieldContext -> 5 },
                "bar(x:\$x)",
                variables = listOf(FromArgumentVariable("x", "x"))
            )
            .resolver("Query" to "bar") { 3 }
            .let {
                assertThrows<GraphQLBuildError> {
                    it.build()
                }
                Unit
            }

    @Test
    fun `from arg -- arg from operation variable`() =
        FeatureTestBuilder()
            .sdl("type Query { foo(y:Int!):Int!, bar(x:Int!): Int! }")
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar") * 5 },
                "bar(x:\$y)",
                variables = listOf(
                    FromArgumentVariable("y", "y")
                )
            )
            .resolver("Query" to "bar") { it.arguments.get<Int>("x") * 3 }
            .build()
            .assertJson(
                "{data: {foo: 30}}",
                "query Q(\$vary:Int!) {foo(y:\$vary)}",
                mapOf("vary" to 2)
            )

    @Test
    fun `same variable name used in same argument with different values`() =
        // The spirit of this test is that baz is queried multiple times with "x:$x", but
        // with different values of $x. We want to test that we are not memoizing bar on the
        // reference to variable $x, but rather the value of variable $x
        FeatureTestBuilder()
            .sdl("type Query { foo(x:Int):Int, bar(x:Int):Int, baz(x:Int):Int }")
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("baz") * 11 },
                "baz(x:\$x)",
                variables = listOf(FromArgumentVariable("x", "x"))
            )
            .resolver(
                "Query" to "bar",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("baz") * 7 },
                "baz(x:\$x)",
                variables = listOf(FromArgumentVariable("x", "x"))
            )
            .resolver("Query" to "baz") { it.arguments.get<Int>("x") * 5 }
            .build()
            .assertJson("{data:{foo:110, bar:105}}", "{foo(x:2) bar(x:3)}")

    @Test
    fun `from query field -- invalid path`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: String!, bar: String! }")
            .resolver(
                "Query" to "bar",
                { _: UntypedFieldContext -> "result" },
                queryValueFragment = "fragment _ on Query { foo }",
                variables = listOf(FromQueryFieldVariable("invalid", "invalidField"))
            )
            .resolver("Query" to "foo") { "test" }
            .let {
                assertThrows<Exception> { it.build() }
                Unit
            }

    @Disabled("Disabled until we have QueryFieldVariable support in execution.")
    @Test
    fun `from query field -- simple`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: Int!, bar(x: Int!): Int! }")
            .resolver(
                "Query" to "bar",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("foo") * 5 },
                "foo(x:\$fooVar)",
                queryValueFragment = "fragment _ on Query { foo }",
                variables = listOf(FromQueryFieldVariable("fooVar", "foo"))
            )
            .resolver("Query" to "foo") { it.arguments.get<Int>("x") * 3 }
            .build()
            .assertJson("{data: {bar: 45}}", "{bar(x:2)}")

    @Disabled("Disabled until we have QueryFieldVariable support in execution.")
    @Test
    fun `from query field -- binds variable to query field with different name`() =
        FeatureTestBuilder()
            .sdl("type Query { baz: Int!, bar(x: Int!): Int! }")
            .resolver(
                "Query" to "bar",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("baz") * 7 },
                "baz(x:\$bazValue)",
                queryValueFragment = "fragment _ on Query { baz }",
                variables = listOf(FromQueryFieldVariable("bazValue", "baz"))
            )
            .resolver("Query" to "baz") { it.arguments.get<Int>("x") * 4 }
            .build()
            .assertJson("{data: {bar: 112}}", "{bar(x:1)}")

    @Disabled("Disabled until we have QueryFieldVariable support in execution.")
    @Test
    fun `from query field -- path traversal`() =
        FeatureTestBuilder()
            .sdl(
                """
                    input UserInput { name: String }
                    type Query { viewer(input: UserInput): String, bar(name: String!): Int! }
                """.trimIndent()
            )
            .resolver(
                "Query" to "bar",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<String>("viewer").length * 2 },
                "viewer(input: {name: \$viewerName})",
                queryValueFragment = "fragment _ on Query { viewer(input: {name: \"test\"}) }",
                variables = listOf(FromQueryFieldVariable("viewerName", "viewer"))
            )
            .resolver("Query" to "viewer") { it.arguments.tryGet<Map<String, Any>>("input")?.get("name") ?: "default" }
            .build()
            .assertJson("{data: {bar: 8}}", "{bar(name:\"someInput\")}")

    @Disabled("Disabled until we have QueryFieldVariable support in execution.")
    @Test
    fun `from query field -- returns null value`() =
        FeatureTestBuilder()
            .sdl("type Query { viewerOrNull: String, bar(name: String): Int! }")
            .resolver(
                "Query" to "bar",
                { _: UntypedFieldContext -> 42 },
                "bar(name:\$viewerName)",
                queryValueFragment = "fragment _ on Query { viewerOrNull }",
                variables = listOf(FromQueryFieldVariable("viewerName", "viewerOrNull"))
            )
            .resolver("Query" to "viewerOrNull") { null }
            .build()
            .assertJson("{data: {bar: 42}}", "{bar}")

    @Disabled("Disabled until we have QueryFieldVariable support in execution.")
    @Test
    fun `from query field -- variable name overlaps with object field variable`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: String!, bar(x: String!): String! }")
            .resolver(
                "Query" to "bar",
                { _: UntypedFieldContext -> "result" },
                "fragment _ on Query { foo }",
                queryValueFragment = "fragment _ on Query { foo }",
                variables = listOf(
                    FromObjectFieldVariable("name", "foo"),
                    FromQueryFieldVariable("name", "foo")
                )
            )
            .resolver("Query" to "foo") { "test" }
            .let {
                assertThrows<Exception> { it.build() }
                Unit
            }

    @Disabled("Disabled until we have QueryFieldVariable support in execution.")
    @Test
    fun `from query field -- variable name overlaps with argument variable`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: String!, bar(name: String!): String! }")
            .resolver(
                "Query" to "bar",
                { _: UntypedFieldContext -> "result" },
                queryValueFragment = "fragment _ on Query { foo }",
                variables = listOf(
                    FromArgumentVariable("name", "name"),
                    FromQueryFieldVariable("name", "foo")
                )
            )
            .resolver("Query" to "foo") { "test" }
            .let {
                assertThrows<Exception> { it.build() }
                Unit
            }

    @Disabled("Disabled until we have QueryFieldVariable support in execution.")
    @Test
    fun `mixed variables -- from query field and from argument`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: Int!, bar(userId: String!, x: Int!): Int! }")
            .resolver(
                "Query" to "bar",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("foo") * ctx.arguments.get<String>("userId").length },
                "foo(x:\$fooValue)",
                queryValueFragment = "fragment _ on Query { foo }",
                variables = listOf(
                    FromArgumentVariable("userId", "userId"),
                    FromQueryFieldVariable("fooValue", "foo")
                )
            )
            .resolver("Query" to "foo") { it.arguments.get<Int>("x") * 2 }
            .build()
            .assertJson("{data: {bar: 40}}", "{bar(userId:\"test\")}")

    @Disabled("Disabled until we have QueryFieldVariable support in execution.")
    @Test
    fun `mixed variables -- from query field and from object field`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: Int!, baz(name: String!): String!, bar(x: Int!): Int! }")
            .resolver(
                "Query" to "bar",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("foo") * ctx.objectValue.get<String>("baz").length },
                "foo(x:\$fooValue), baz(name:\$bazValue)",
                queryValueFragment = "fragment _ on Query { foo }",
                variables = listOf(
                    FromObjectFieldVariable("bazValue", "baz"),
                    FromQueryFieldVariable("fooValue", "foo")
                )
            )
            .resolver("Query" to "foo") { it.arguments.get<Int>("x") * 5 }
            .resolver("Query" to "baz") { it.arguments.get<String>("name") + it.arguments.get<String>("name") }
            .build()
            .assertJson("{data: {bar: 10}}", "{bar(x:1)}")
}
