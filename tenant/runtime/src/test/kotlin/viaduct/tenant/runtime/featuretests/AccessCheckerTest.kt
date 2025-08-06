package viaduct.tenant.runtime.featuretests

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import viaduct.api.context.FieldExecutionContext
import viaduct.api.types.Arguments
import viaduct.api.types.CompositeOutput
import viaduct.tenant.runtime.featuretests.fixtures.Baz
import viaduct.tenant.runtime.featuretests.fixtures.Boo
import viaduct.tenant.runtime.featuretests.fixtures.FeatureTestBuilder
import viaduct.tenant.runtime.featuretests.fixtures.FeatureTestSchemaFixture
import viaduct.tenant.runtime.featuretests.fixtures.Query
import viaduct.tenant.runtime.featuretests.fixtures.UntypedFieldContext
import viaduct.tenant.runtime.featuretests.fixtures.UntypedMutationFieldContext
import viaduct.tenant.runtime.featuretests.fixtures.UntypedNodeContext
import viaduct.tenant.runtime.featuretests.fixtures.assertData

@ExperimentalCoroutinesApi
class AccessCheckerTest {
    private fun builder() = FeatureTestBuilder().sdl(FeatureTestSchemaFixture.sdl)

    // Currently we only register type checkers for nodes with node resolvers
    private fun builderForTypeChecks() =
        builder()
            .resolver(
                "Query" to "bazList",
                { ctx: UntypedFieldContext ->
                    listOf(
                        ctx.nodeFor(ctx.globalIDFor(Baz.Reflection, "1")),
                        ctx.nodeFor(ctx.globalIDFor(Baz.Reflection, "2")),
                        ctx.nodeFor(ctx.globalIDFor(Baz.Reflection, "3"))
                    )
                }
            )
            .resolver(
                "Query" to "baz",
                { ctx: UntypedFieldContext -> ctx.nodeFor(ctx.globalIDFor(Baz.Reflection, "1")) }
            )
            .nodeResolver(
                "Baz",
                { ctx: UntypedNodeContext -> Baz.Builder(ctx).x(ctx.id.internalID.toInt()).build() }
            )
            .resolver(
                "Baz" to "y",
                { ctx: FieldExecutionContext<Baz, Query, Arguments.NoArguments, CompositeOutput.NotComposite> -> ctx.objectValue.getX().toString() },
                "x"
            )

    @Test
    fun `no checkers`() =
        builder()
            .resolver(
                "Query" to "boo",
                { ctx: UntypedFieldContext -> Boo.Builder(ctx).value(5).build() },
            )
            .build()
            .assertJson("{data: {boo: {value : 5}}}", "{ boo { value } }")

    @Test
    fun `checkers are executed for both sync and async fields`() {
        var asyncFieldCheckerRan = false
        var syncFieldCheckerRan = false
        builder()
            .resolver(
                "Query" to "boo",
                { ctx: UntypedFieldContext -> Boo.Builder(ctx).value(5).build() },
            )
            .fieldChecker(
                "Query" to "boo",
                executeFn = { args, pobj -> asyncFieldCheckerRan = true }
            )
            .fieldChecker(
                "Boo" to "value",
                executeFn = { args, pobj -> syncFieldCheckerRan = true }
            )
            .build()
            .assertJson("{data: {boo: {value: 5}}}", "{ boo { value } }")
            .also {
                assertTrue(asyncFieldCheckerRan)
                // TODO: uncomment after registering all checkers
                // assertTrue(syncFieldCheckerRan)
            }
    }

    @Test
    fun `async field successful, checker throws`() {
        builder()
            .resolver(
                "Query" to "boo",
                { ctx: UntypedFieldContext -> Boo.Builder(ctx).value(5).build() },
            )
            .fieldChecker(
                "Query" to "boo",
                executeFn = { args, pobj -> throw RuntimeException("permission denied") }
            )
            .build()
            .execute("{ boo { value } }")
            .assertData("{boo: null}") { errors ->
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("boo"), error.path)
                    assertTrue(error.message.contains("permission denied"))
                }
            }
    }

    // TODO: uncomment after registering all checkers
    // @Test
    // fun `sync field successful, checker throws`() {
    //     builder()
    //         .resolver(
    //             "Query" to "boo",
    //             { ctx: UntypedFieldContext -> Boo.Builder(ctx).value(5).build() },
    //         )
    //         .fieldChecker(
    //             "Boo" to "value",
    //             executeFn = { args, pobj -> throw RuntimeException("permission denied") }
    //         )
    //         .build()
    //         .execute("{ boo { value } }")
    //         .assertData("{boo: {value: null}}") { errors ->
    //             assertEquals(1, errors.size)
    //             errors[0].let { error ->
    //                 assertEquals(listOf("boo", "value"), error.path)
    //                 assertTrue(error.message.contains("permission denied"))
    //             }
    //         }
    // }

    @Test
    fun `async field throws, checker throws`() {
        builder()
            .resolver(
                "Query" to "boo",
                { ctx: UntypedFieldContext -> throw RuntimeException("not found") },
            )
            .fieldChecker(
                "Query" to "boo",
                executeFn = { args, pobj -> throw RuntimeException("permission denied") }
            )
            .build()
            .execute("{ boo { value } }")
            .assertData("{boo: null}") { errors ->
                // resolver error takes priority
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("boo"), error.path)
                    assertTrue(error.message.contains("not found"))
                }
            }
    }

    @Test
    fun `sync field throws, checker throws`() {
        class FakeBoo {
            fun getValue(): Int = throw RuntimeException("FakeBoo.value is throwing")
        }
        builder()
            .resolver(
                "Query" to "boo",
                { ctx: UntypedFieldContext -> FakeBoo() },
            )
            .fieldChecker(
                "Boo" to "value",
                executeFn = { args, pobj -> throw RuntimeException("permission denied") }
            )
            .build()
            .execute("{ boo { value } }")
            .assertData("{boo: { value: null }}") { errors ->
                // datafetcher error takes priority
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("boo", "value"), error.path)
                    assertTrue(error.message.contains("FakeBoo.value is throwing"))
                }
            }
    }

    @Test
    fun `objectValueFragment field - field and checker successful`() {
        builder()
            .resolver(
                "Query" to "string1",
                { ctx: FieldExecutionContext<Query, Query, Arguments.NoArguments, CompositeOutput.NotComposite> ->
                    val value = ctx.objectValue.getString2()
                    "1st & $value"
                },
                "string2"
            )
            .resolver(
                "Query" to "string2",
                { ctx: UntypedFieldContext -> "2nd" },
            )
            .fieldChecker(
                "Query" to "string2",
                executeFn = { _, _ -> /* access granted */ }
            )
            .build()
            .assertJson("{data: {string1: \"1st & 2nd\"}}", "{ string1 }")
    }

    @Test
    fun `objectValueFragment field - field successful, checker throws`() {
        builder()
            .resolver(
                "Query" to "string1",
                { ctx: FieldExecutionContext<Query, Query, Arguments.NoArguments, CompositeOutput.NotComposite> ->
                    val value = ctx.objectValue.getString2()
                    "1st & $value"
                },
                "string2"
            )
            .resolver(
                "Query" to "string2",
                { ctx: UntypedFieldContext -> "2nd" },
            )
            .fieldChecker(
                "Query" to "string2",
                executeFn = { _, _ -> throw RuntimeException("string2 permission denied") }
            )
            .build()
            .execute("{ string1 }")
            .assertData("{string1: null}") { errors ->
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("string1"), error.path)
                    assertTrue(error.message.contains("string2 permission denied"))
                }
            }
    }

    @Test
    fun `objectValueFragment field - field throws, checker throws`() {
        builder()
            .resolver(
                "Query" to "string1",
                { ctx: FieldExecutionContext<Query, Query, Arguments.NoArguments, CompositeOutput.NotComposite> ->
                    val value = ctx.objectValue.getString2()
                    "1st & $value"
                },
                "string2"
            )
            .resolver(
                "Query" to "string2",
                { ctx: UntypedFieldContext -> throw RuntimeException("string2 resolver throws") },
            )
            .fieldChecker(
                "Query" to "string2",
                executeFn = { _, _ -> throw RuntimeException("string2 permission denied") }
            )
            .build()
            .execute("{ string1 }")
            .assertData("{string1: null}") { errors ->
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("string1"), error.path)
                    // datafetcher error takes priority
                    assertTrue(error.message.contains("string2 resolver throws"))
                }
            }
    }

    @Test
    fun `objectValueFragment field - field throws, checker successful`() {
        builder()
            .resolver(
                "Query" to "string1",
                { ctx: FieldExecutionContext<Query, Query, Arguments.NoArguments, CompositeOutput.NotComposite> ->
                    val value = ctx.objectValue.getString2()
                    "1st & $value"
                },
                "string2"
            )
            .resolver(
                "Query" to "string2",
                { ctx: UntypedFieldContext -> throw RuntimeException("string2 resolver throws") },
            )
            .fieldChecker(
                "Query" to "string2",
                executeFn = { _, _ -> /* access granted */ }
            )
            .build()
            .execute("{ string1 }")
            .assertData("{string1: null}") { errors ->
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("string1"), error.path)
                    assertTrue(error.message.contains("string2 resolver throws"))
                }
            }
    }

    @Test
    fun `checker with rss - access checks skipped`() {
        builder()
            .resolver(
                "Query" to "string1",
                { ctx: UntypedFieldContext -> "foo" }
            )
            .resolver(
                "Query" to "string2",
                { ctx: UntypedFieldContext -> "bar" }
            )
            .fieldChecker(
                "Query" to "string1",
                executeFn = { _, objectDataMap ->
                    if (objectDataMap["key"]?.fetch("string2") == "bar") {
                        // Verifies that the access check for string2 shouldn't be applied
                        // during fetch, otherwise the error would be "string2 checker failed"
                        throw RuntimeException("this should get thrown")
                    }
                },
                Triple("key", "Query", "string2")
            )
            .fieldChecker(
                "Query" to "string2",
                executeFn = { _, _ -> throw RuntimeException("string2 checker failed") },
            )
            .build()
            .execute("{ string1 }")
            .assertData("{string1: null}") { errors ->
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("string1"), error.path)
                    assertTrue(error.message.contains("this should get thrown"))
                }
            }
    }

    @Test
    fun `mutation field with checker`() {
        var mutationResolverRan = false
        builder()
            .resolver(
                "Mutation" to "string1",
                { ctx: UntypedMutationFieldContext ->
                    mutationResolverRan = true
                    "foo"
                }
            )
            .fieldChecker(
                "Mutation" to "string1",
                executeFn = { _, _ -> throw RuntimeException("string1 checker failed") },
            )
            .build()
            .execute("mutation { string1 }")
            .assertData("{string1: null}") { errors ->
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("string1"), error.path)
                    assertTrue(error.message.contains("string1 checker failed"))
                }
            }
        assertFalse(mutationResolverRan)
    }

    fun `field and type checks fail`() {
        builderForTypeChecks()
            .fieldChecker(
                "Query" to "baz",
                executeFn = { _, _ -> throw RuntimeException("field checker failed") }
            )
            .typeChecker(
                "Baz",
                executeFn = { _, _ -> throw RuntimeException("type checker failed") }
            )
            .build()
            .execute("{ baz { id } }")
            .assertData("{ baz: null }") { errors ->
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("baz"), error.path)
                    assertTrue(error.message.contains("field checker failed"))
                }
            }
    }

    @Test
    fun `field check succeeds, type check fails`() {
        builderForTypeChecks()
            .fieldChecker("Query" to "baz", executeFn = { _, _ -> /* access granted */ })
            .typeChecker(
                "Baz",
                executeFn = { _, _ -> throw RuntimeException("type checker failed") }
            )
            .build()
            .execute("{ baz { id } }")
            .assertData("{ baz: null }") { errors ->
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("baz"), error.path)
                    assertTrue(error.message.contains("type checker failed"))
                }
            }
    }

    @Test
    fun `no field check, type check fails`() {
        builderForTypeChecks()
            .typeChecker(
                "Baz",
                executeFn = { _, _ -> throw RuntimeException("type checker failed") }
            )
            .build()
            .execute("{ baz { id } }")
            .assertData("{ baz: null }") { errors ->
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("baz"), error.path)
                    assertTrue(error.message.contains("type checker failed"))
                }
            }
    }

    @Test
    fun `type check on interface field`() {
        builderForTypeChecks()
            .resolver(
                "Query" to "node",
                { ctx: UntypedFieldContext -> ctx.nodeFor(ctx.globalIDFor(Baz.Reflection, "1")) }
            )
            .typeChecker(
                "Baz",
                executeFn = { _, _ -> throw RuntimeException("type checker failed") }
            )
            .build()
            .execute("{ node { id } }")
            .assertData("{ node: null }") { errors ->
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("node"), error.path)
                    assertTrue(error.message.contains("type checker failed"))
                }
            }
    }

    @Test
    fun `type check with rss`() {
        builderForTypeChecks()
            .resolver(
                "Baz" to "y",
                { ctx: FieldExecutionContext<Baz, Query, Arguments.NoArguments, CompositeOutput.NotComposite> -> ctx.objectValue.getX().toString() },
                "x"
            )
            .typeChecker(
                "Baz",
                executeFn = { _, objectDataMap ->
                    if (objectDataMap["key"]!!.fetch("y") == "1") {
                        // Verifies that this got the correct value for a dependent field
                        // without a circular dep on the type checker
                        throw RuntimeException("this should get thrown")
                    }
                },
                Triple("key", "Baz", "y")
            )
            .build()
            // TODO: remove "y" from the query here once we add type checker RSSs to the query plan
            .execute("{ baz { id y } }")
            .assertData("{ baz: null }") { errors ->
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("baz"), error.path)
                    assertTrue(error.message.contains("this should get thrown"))
                }
            }
    }

    @Test
    fun `type checks for list of objects`() {
        builderForTypeChecks()
            .typeChecker(
                "Baz",
                executeFn = { _, objectDataMap ->
                    val eod = objectDataMap["key"]!!
                    val y = eod.fetch("y")
                    if (y == "2") {
                        throw RuntimeException("permission denied for baz with internal ID 2")
                    }
                },
                Triple("key", "Baz", "y")
            )
            .build()
            // TODO: remove "y" from the query here once we add type checker RSSs to the query plan
            .execute("{ bazList { id y } }")
            .assertData("{ bazList: [{id:\"QmF6OjE=\",y:\"1\"}, null, {id:\"QmF6OjM=\",y:\"3\"}] }") { errors ->
                assertEquals(1, errors.size)
                errors[0].let { error ->
                    assertEquals(listOf("bazList", 1), error.path)
                    assertTrue(error.message.contains("permission denied for baz with internal ID 2"))
                }
            }
    }
}
