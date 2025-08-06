package viaduct.tenant.runtime.featuretests

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.api.context.FieldExecutionContext
import viaduct.api.types.Arguments
import viaduct.api.types.CompositeOutput
import viaduct.engine.api.GraphQLBuildError
import viaduct.engine.runtime.tenantloading.RequiredSelectionsAreInvalid
import viaduct.tenant.runtime.featuretests.fixtures.Bar
import viaduct.tenant.runtime.featuretests.fixtures.Baz
import viaduct.tenant.runtime.featuretests.fixtures.FeatureTestBuilder
import viaduct.tenant.runtime.featuretests.fixtures.FeatureTestSchemaFixture
import viaduct.tenant.runtime.featuretests.fixtures.Query
import viaduct.tenant.runtime.featuretests.fixtures.UntypedFieldContext

@ExperimentalCoroutinesApi
class RequiredSelectionsTest {
    @Test
    fun `resolve field with required sibling field`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: String, bar: String }")
            .resolver("Query" to "bar") { "BAR" }
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext ->
                    ctx.objectValue.get<String>("bar", String::class).reversed()
                },
                objectValueFragment = "bar"
            )
            .build()
            .assertJson("{data: {foo: \"RAB\"}}", "{foo}")

    @Test
    fun `resolve field with transitive required selections`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: Int, bar: Int, baz: Int }")
            .resolver("Query" to "baz") { 2 }
            .resolver(
                "Query" to "bar",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("baz", Int::class) * 3 },
                objectValueFragment = "baz"
            )
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar", Int::class) * 5 },
                objectValueFragment = "bar"
            )
            .build()
            .assertJson("{data: {foo: 30}}", "{foo}")

    @Test
    fun `required selections use aliases`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: Int, bar: Int }")
            .resolver("Query" to "bar") { 3 }
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar", "aliasedBar") * 2 },
                objectValueFragment = "aliasedBar: bar"
            )
            .build()
            .assertJson("{data: {foo: 6}}", "{foo}")

    @Test
    fun `required selections use deep aliases`() =
        FeatureTestBuilder()
            .grtPackage(Query.Reflection)
            .sdl(FeatureTestSchemaFixture.sdl)
            .resolver(
                "Query" to "string1",
                { ctx: FieldExecutionContext<Query, Query, Arguments.NoArguments, CompositeOutput.NotComposite> ->
                    val value = ctx.objectValue.getBar("aliasedBar")?.getValue("aliasedValue")
                    "A:$value"
                },
                objectValueFragment = "aliasedBar: bar { aliasedValue: value }"
            )
            .resolver("Query" to "bar") { Bar.Builder(it).value("B").build() }
            .build()
            .assertJson("{data: {string1: \"A:B\"}}", "{string1}")

    @Test
    fun `required selections use arguments`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: Int, bar(x:Int):Int }")
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar") * 2 },
                objectValueFragment = "bar(x:3)"
            )
            .resolver("Query" to "bar") { it.arguments.get<Int>("x") * 5 }
            .build()
            .assertJson("{data: {foo:30}}", "{foo}")

    @Test
    fun `required selections use aliases and arguments`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: Int, bar(x:Int):Int }")
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar", "aliasedBar") * 2 },
                objectValueFragment = "aliasedBar:bar(x:3)"
            )
            .resolver("Query" to "bar") { it.arguments.get<Int>("x") * 5 }
            .build()
            .assertJson("{data: {foo: 30}}", "{foo}")

    @Test
    fun `required selections select an argumented field multiple times`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: Int, bar(x:Int):Int }")
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext ->
                    ctx.objectValue.get<Int>("bar", "b1") *
                        ctx.objectValue.get<Int>("bar", "b2")
                },
                objectValueFragment = "b1:bar(x:3), b2:bar(x:5)"
            )
            .resolver("Query" to "bar") { it.arguments.get<Int>("x") * 2 }
            .build()
            .assertJson("{data: {foo: 60}}", "{foo}")

    @Test
    fun `required selections use fragments`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: Int, bar: Int }")
            .resolver("Query" to "bar") { 3 }
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar", Int::class) * 2 },
                objectValueFragment = "fragment _ on Query { bar }"
            )
            .build()
            .assertJson("{data: {foo: 6}}", "{foo}")

    @Test
    fun `required selections use untyped inline fragments`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: Int, bar: Int }")
            .resolver("Query" to "bar") { 3 }
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar", Int::class) * 2 },
                objectValueFragment = "... { bar }"
            )
            .build()
            .assertJson("{data: {foo: 6}}", "{foo}")

    @Test
    fun `required selections use typed inline fragments`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: Int, bar: Int }")
            .resolver("Query" to "bar") { 3 }
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar", Int::class) * 2 },
                objectValueFragment = "... on Query { bar }"
            )
            .build()
            .assertJson("{data: {foo: 6}}", "{foo}")

    @Test
    fun `resolve fields with shared requirement`() {
        val bazCount = AtomicInteger()
        FeatureTestBuilder()
            .sdl("type Query { foo: Int, bar: Int, baz: Int }")
            .resolver("Query" to "baz") { bazCount.incrementAndGet().let { 5 } }
            .resolver(
                "Query" to "bar",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("baz", Int::class) * 3 },
                objectValueFragment = "baz"
            )
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("baz", Int::class) * 2 },
                objectValueFragment = "baz"
            )
            .build()
            .assertJson("{data: {foo: 10, bar: 15}}", "{foo bar}")
            .also { assertEquals(1, bazCount.get()) }
    }

    @Test
    fun `resolve field with multiple requirements`() =
        FeatureTestBuilder()
            .sdl("type Query { foo: Int, bar: Int, baz: Int }")
            .resolver("Query" to "baz") { 5 }
            .resolver("Query" to "bar") { 3 }
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext ->
                    ctx.objectValue.get<Int>("bar", Int::class) * ctx.objectValue.get<Int>("baz", Int::class)
                },
                objectValueFragment = "bar baz"
            )
            .build()
            .assertJson("{data: {foo: 15}}", "{foo}")

    @Test
    fun `resolve fields multiple mergeable requirements`() {
        val barCount = AtomicInteger()
        FeatureTestBuilder()
            .sdl("type Query { foo: Int, bar: Int }")
            .resolver("Query" to "bar") { 3.also { barCount.incrementAndGet() } }
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("bar", Int::class) * 2 },
                objectValueFragment = """
                    fragment F on Query { bar }
                    fragment Main on Query {
                      bar
                      aliasedBar: bar
                      ... {
                        bar
                        ... {
                          bar
                          ... F
                        }
                      }
                      ... on Query {
                        bar
                        ... on Query {
                          bar
                          ... F
                        }
                      }
                      ... F
                    }
                """.trimIndent()
            )
            .build()
            .assertJson("{data: {foo: 6, bar: 3}}", "{foo bar}")
            .also { assertEquals(2, barCount.get()) }
    }

    @Test
    fun `resolve private field in RSS`() {
        FeatureTestBuilder()
            // bar is a private field that is only visible in full schema
            .sdl("type Query { foo: Int, bar: Int}")
            .scopedSchemaSdl("type Query { foo: Int }")
            .resolver("Query" to "bar") { 3 }
            .resolver(
                "Query" to "foo",
                { ctx: UntypedFieldContext ->
                    ctx.objectValue.get<Int>("bar", Int::class) + 1
                },
                objectValueFragment = "bar"
            )
            .build()
            .assertJson("{data: {foo: 4}}", "{foo}")
    }

    @Test
    fun `resolve field with queryValueFragment - simple field access`() =
        FeatureTestBuilder()
            .sdl("type Query { currentUser: String, userGreeting: String }")
            .resolver("Query" to "currentUser") { "Alice" }
            .resolver(
                "Query" to "userGreeting",
                { ctx: UntypedFieldContext ->
                    val user = ctx.queryValue.get<String>("currentUser", String::class)
                    "Hello, $user!"
                },
                queryValueFragment = "currentUser"
            )
            .build()
            .assertJson("{data: {userGreeting: \"Hello, Alice!\"}}", "{userGreeting}")

    @Test
    fun `resolve field with queryValueFragment - with aliases`() =
        FeatureTestBuilder()
            .sdl("type Query { currentUser: String, userCount: Int, summary: String }")
            .resolver("Query" to "currentUser") { "Bob" }
            .resolver("Query" to "userCount") { 42 }
            .resolver(
                "Query" to "summary",
                { ctx: UntypedFieldContext ->
                    val user = ctx.queryValue.get<String>("currentUser", "user")
                    val count = ctx.queryValue.get<Int>("userCount", "count")
                    "$user has $count items"
                },
                queryValueFragment = "user: currentUser, count: userCount"
            )
            .build()
            .assertJson("{data: {summary: \"Bob has 42 items\"}}", "{summary}")

    @Test
    fun `resolve field with queryValueFragment - with arguments`() =
        FeatureTestBuilder()
            .sdl("type Query { user(id: String!): String, userMessage: String }")
            .resolver("Query" to "user") { ctx ->
                val id = ctx.arguments.get<String>("id")
                "User-$id"
            }
            .resolver(
                "Query" to "userMessage",
                { ctx: UntypedFieldContext ->
                    val user = ctx.queryValue.get<String>("user")
                    "Message for: $user"
                },
                queryValueFragment = "user(id: \"123\")"
            )
            .build()
            .assertJson("{data: {userMessage: \"Message for: User-123\"}}", "{userMessage}")

    @Test
    fun `resolve field with queryValueFragment - using fragments`() =
        FeatureTestBuilder()
            .sdl("type Query { userName: String, userEmail: String, profile: String }")
            .resolver("Query" to "userName") { "Charlie" }
            .resolver("Query" to "userEmail") { "charlie@example.com" }
            .resolver(
                "Query" to "profile",
                { ctx: UntypedFieldContext ->
                    val name = ctx.queryValue.get<String>("userName", String::class)
                    val email = ctx.queryValue.get<String>("userEmail", String::class)
                    "Name: $name, Email: $email"
                },
                queryValueFragment = "fragment UserInfo on Query { userName userEmail }"
            )
            .build()
            .assertJson("{data: {profile: \"Name: Charlie, Email: charlie@example.com\"}}", "{profile}")

    @Test
    fun `resolve field with queryValueFragment and objectValueFragment together`() =
        FeatureTestBuilder()
            .grtPackage(Query.Reflection)
            .sdl(FeatureTestSchemaFixture.sdl + "\nextend type Query { globalConfig: String }")
            .resolver("Query" to "globalConfig") { "Premium" }
            .resolver("Query" to "baz") { Baz.Builder(it).id(it.globalIDFor(Baz.Reflection, "baz1")).x(100).build() }
            .resolver(
                "Baz" to "y",
                { ctx: FieldExecutionContext<Baz, Query, Arguments.NoArguments, CompositeOutput.NotComposite> ->
                    val config = ctx.queryValue.get<String>("globalConfig", String::class)
                    val x = ctx.objectValue.getX()
                    "$config item with value $x"
                },
                objectValueFragment = "x",
                queryValueFragment = "globalConfig"
            )
            .build()
            .assertJson(
                "{data: {baz: {y: \"Premium item with value 100\"}}}",
                "{baz { y }}"
            )

    @Test
    fun `resolve field with queryValueFragment - transitive dependencies`() =
        FeatureTestBuilder()
            .sdl("type Query { baseValue: Int, multipliedValue: Int, finalValue: Int }")
            .resolver("Query" to "baseValue") { 5 }
            .resolver(
                "Query" to "multipliedValue",
                { ctx: UntypedFieldContext ->
                    ctx.queryValue.get<Int>("baseValue", Int::class) * 2
                },
                queryValueFragment = "baseValue"
            )
            .resolver(
                "Query" to "finalValue",
                { ctx: UntypedFieldContext ->
                    ctx.queryValue.get<Int>("multipliedValue", Int::class) + 10
                },
                queryValueFragment = "multipliedValue"
            )
            .build()
            .assertJson("{data: {finalValue: 20}}", "{finalValue}")

    @Test
    fun `resolve field with queryValueFragment - multiple query selections`() {
        val userCount = AtomicInteger()
        val configCount = AtomicInteger()
        FeatureTestBuilder()
            .sdl("type Query { currentUser: String, globalConfig: String, combined: String }")
            .resolver("Query" to "currentUser") {
                userCount.incrementAndGet()
                "David"
            }
            .resolver("Query" to "globalConfig") {
                configCount.incrementAndGet()
                "Advanced"
            }
            .resolver(
                "Query" to "combined",
                { ctx: UntypedFieldContext ->
                    val user = ctx.queryValue.get<String>("currentUser", String::class)
                    val config = ctx.queryValue.get<String>("globalConfig", String::class)
                    "$user - $config mode"
                },
                queryValueFragment = "currentUser globalConfig"
            )
            .build()
            .assertJson("{data: {combined: \"David - Advanced mode\"}}", "{combined}")
            .also {
                assertEquals(1, userCount.get(), "currentUser should be resolved only once")
                assertEquals(1, configCount.get(), "globalConfig should be resolved only once")
            }
    }

    @Test
    fun `resolve field with queryValueFragment - inline fragment without type condition`() =
        FeatureTestBuilder()
            .sdl("type Query { isEnabled: Boolean, config: String, result: String }")
            .resolver("Query" to "isEnabled") { true }
            .resolver("Query" to "config") { "production" }
            .resolver(
                "Query" to "result",
                { ctx: UntypedFieldContext ->
                    val enabled = ctx.queryValue.get<Boolean>("isEnabled", Boolean::class)
                    val config = ctx.queryValue.get<String>("config", String::class)
                    if (enabled) "Running in $config" else "Disabled"
                },
                queryValueFragment = "... { isEnabled config }"
            )
            .build()
            .assertJson("{data: {result: \"Running in production\"}}", "{result}")

    @Test
    fun `resolve field with queryValueFragment - handles null gracefully`() =
        FeatureTestBuilder()
            .sdl("type Query { optionalValue: String, result: String }")
            .resolver("Query" to "optionalValue") { null }
            .resolver(
                "Query" to "result",
                { ctx: UntypedFieldContext ->
                    val value = ctx.queryValue.get<String?>("optionalValue", String::class)
                    value ?: "No value provided"
                },
                queryValueFragment = "optionalValue"
            )
            .build()
            .assertJson("{data: {result: \"No value provided\"}}", "{result}")

    @Test
    fun `resolve mutation with queryValueFragment`() =
        FeatureTestBuilder()
            .grtPackage(Query.Reflection)
            .sdl(FeatureTestSchemaFixture.sdl)
            .resolver("Query" to "string1") { "InitialValue" }
            .resolver(
                "Mutation" to "string1",
                { ctx: FieldExecutionContext<Query, Query, Arguments.NoArguments, CompositeOutput.NotComposite> ->
                    val currentValue = ctx.queryValue.getString1()
                    "Mutated from: $currentValue"
                },
                queryValueFragment = "string1"
            )
            .build()
            .assertJson(
                "{data: {string1: \"Mutated from: InitialValue\"}}",
                "mutation { string1 }"
            )

    @Test
    fun `resolve field with queryValueFragment - nested object access`() =
        FeatureTestBuilder()
            .grtPackage(Query.Reflection)
            .sdl(FeatureTestSchemaFixture.sdl)
            .resolver(
                "Baz" to "y",
                { ctx: FieldExecutionContext<Baz, Query, Arguments.NoArguments, CompositeOutput.NotComposite> ->
                    val barValue = ctx.queryValue.getBar()?.getValue()
                    "Baz sees bar value: $barValue"
                },
                queryValueFragment = "bar { value }"
            )
            .resolver("Query" to "bar") { Bar.Builder(it).build() }
            .resolver("Bar" to "value") { "BarValue" }
            .resolver("Query" to "baz") { Baz.Builder(it).id(it.globalIDFor(Baz.Reflection, "")).x(10).build() }
            .build()
            .assertJson("{data: {baz: {y: \"Baz sees bar value: BarValue\"}}}", "{baz { y }}")

    @Test
    fun `resolve field with queryValueFragment - typed inline fragment`() =
        FeatureTestBuilder()
            .sdl("type Query { enabled: Boolean, message: String, status: String }")
            .resolver("Query" to "enabled") { false }
            .resolver("Query" to "message") { "System offline" }
            .resolver(
                "Query" to "status",
                { ctx: UntypedFieldContext ->
                    val enabled = ctx.queryValue.get<Boolean>("enabled", Boolean::class)
                    val message = ctx.queryValue.get<String>("message", String::class)
                    if (!enabled) message else "OK"
                },
                queryValueFragment = "... on Query { enabled message }"
            )
            .build()
            .assertJson("{data: {status: \"System offline\"}}", "{status}")

    @Test
    fun `queryValueFragment with unclosed brace should fail at build time`() {
        assertThrows<IllegalArgumentException> {
            FeatureTestBuilder()
                .sdl("type Query { field: String, result: String }")
                .resolver("Query" to "field") { "value" }
                .resolver(
                    "Query" to "result",
                    { _: UntypedFieldContext -> "should not execute" },
                    queryValueFragment = "{ field" // Missing closing brace
                )
                .build()
        }
    }

    @Test
    fun `queryValueFragment with invalid field syntax should fail at build time`() {
        assertThrows<IllegalArgumentException> {
            FeatureTestBuilder()
                .sdl("type Query { field: String, result: String }")
                .resolver("Query" to "field") { "value" }
                .resolver(
                    "Query" to "result",
                    { _: UntypedFieldContext -> "should not execute" },
                    queryValueFragment = "field(" // Invalid - parenthesis without arguments
                )
                .build()
        }
    }

    @Test
    fun `queryValueFragment referencing non-existent field should fail at build time`() {
        assertThrows<RequiredSelectionsAreInvalid> {
            try {
                FeatureTestBuilder()
                    .sdl("type Query { existingField: String, result: String }")
                    .resolver("Query" to "existingField") { "value" }
                    .resolver(
                        "Query" to "result",
                        { _: UntypedFieldContext -> "should not execute" },
                        queryValueFragment = "nonExistentField" // Field doesn't exist in schema
                    )
                    .build()
            } catch (e: GraphQLBuildError) {
                // unwrap the provision exception...
                throw e.cause?.cause ?: e
            }
        }
    }

    @Test
    fun `queryValueFragment with invalid fragment syntax should fail at build time`() {
        assertThrows<IllegalArgumentException> {
            FeatureTestBuilder()
                .sdl("type Query { field: String, result: String }")
                .resolver("Query" to "field") { "value" }
                .resolver(
                    "Query" to "result",
                    { _: UntypedFieldContext -> "should not execute" },
                    queryValueFragment = "fragment on Query { field }" // Missing fragment name
                )
                .build()
        }
    }

    @Test
    fun `queryValueFragment with invalid variable syntax should fail at build time`() {
        assertThrows<IllegalArgumentException> {
            FeatureTestBuilder()
                .sdl("type Query { field(arg: Int!): String, result: String }")
                .resolver("Query" to "field") { "value" }
                .resolver(
                    "Query" to "result",
                    { _: UntypedFieldContext -> "should not execute" },
                    queryValueFragment = "field(arg: $)" // Invalid variable syntax
                )
                .build()
        }
    }

    @Test
    fun `queryValueFragment with empty selection set should fail at build time`() {
        assertThrows<IllegalArgumentException> {
            FeatureTestBuilder()
                .sdl("type Query { result: String }")
                .resolver(
                    "Query" to "result",
                    { _: UntypedFieldContext -> "should not execute" },
                    queryValueFragment = "{}" // Empty selection set
                )
                .build()
        }
    }

    @Test
    fun `queryValueFragment with wrong type condition should fail at build time`() {
        assertThrows<RequiredSelectionsAreInvalid> {
            try {
                FeatureTestBuilder()
                    .sdl("type Query { field: String, result: String } type Mutation { dummy: String }")
                    .resolver("Query" to "field") { "value" }
                    .resolver(
                        "Query" to "result",
                        { _: UntypedFieldContext -> "should not execute" },
                        queryValueFragment = "... on Mutation { field }" // Wrong type - should be Query
                    )
                    .build()
            } catch (e: GraphQLBuildError) {
                // unwrap the provision exception...
                throw e.cause?.cause ?: e
            }
        }
    }
}
