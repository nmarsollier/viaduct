package viaduct.engine.runtime

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import viaduct.engine.api.EngineObjectData
import viaduct.engine.api.mocks.MockEngineObjectData
import viaduct.engine.api.mocks.MockSchema
import viaduct.engine.api.mocks.MockTenantModuleBootstrapper
import viaduct.engine.api.mocks.runFeatureTest

@ExperimentalCoroutinesApi
class AccessCheckExecutionTest {
    companion object {
        val SDL = """
            interface Node {
                id: ID!
            }

            type Query {
                string1: String
                string2: String
                boo: Boo
                baz: Baz
                bazList: [Baz]
                node: Node
            }

            type Mutation {
                string1: String
            }

            type Boo {
                value: Int
            }

            type Baz implements Node {
                id: ID!
                x: Int
                y: String
            }
        """.trimIndent()

        private val schema = MockSchema.mk(SDL)
        private val booType = schema.schema.getObjectType("Boo")
        private val bazType = schema.schema.getObjectType("Baz")
        private val queryType = schema.schema.getObjectType("Query")
    }

    @Test
    fun `no checkers`() {
        MockTenantModuleBootstrapper(SDL) {
            fieldWithValue("Query" to "boo", MockEngineObjectData(booType, mapOf("value" to 5)))
        }.runFeatureTest {
            viaduct.runQuery("{ boo { value } }")
                .assertJson("{data: {boo: {value: 5}}}")
        }
    }

    @Test
    fun `checkers are executed for both sync and async fields`() {
        var asyncFieldCheckerRan = false
        var syncFieldCheckerRan = false

        MockTenantModuleBootstrapper(SDL) {
            fieldWithValue("Query" to "boo", MockEngineObjectData(booType, mapOf("value" to 5)))
            field("Query" to "boo") {
                checker {
                    fn { _, _ -> asyncFieldCheckerRan = true }
                }
            }
            field("Boo" to "value") {
                checker {
                    fn { _, _ -> syncFieldCheckerRan = true }
                }
            }
        }.runFeatureTest {
            viaduct.runQuery("{ boo { value } }")
                .assertJson("{data: {boo: {value: 5}}}")
        }

        assertTrue(asyncFieldCheckerRan)
        // TODO: uncomment after registering all checkers
        // assertTrue(syncFieldCheckerRan)
    }

    @Test
    fun `async field successful, checker throws`() {
        MockTenantModuleBootstrapper(SDL) {
            fieldWithValue("Query" to "boo", MockEngineObjectData(booType, mapOf("value" to 5)))
            field("Query" to "boo") {
                checker {
                    fn { _, _ -> throw RuntimeException("permission denied") }
                }
            }
        }.runFeatureTest {
            val result = viaduct.runQuery("{ boo { value } }")
            assertEquals(mapOf("boo" to null), result.getData())
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("boo"), error.path)
            assertTrue(error.message.contains("permission denied"))
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
        MockTenantModuleBootstrapper(SDL) {
            field("Query" to "boo") {
                resolver {
                    fn { _, _, _, _, _ -> throw RuntimeException("not found") }
                }
                checker {
                    fn { _, _ -> throw RuntimeException("permission denied") }
                }
            }
        }.runFeatureTest {
            val result = viaduct.runQuery("{ boo { value } }")
            assertEquals(mapOf("boo" to null), result.getData())
            // resolver error takes priority
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("boo"), error.path)
            assertTrue(error.message.contains("not found"))
        }
    }

    @Test
    fun `sync field throws, checker throws`() {
        MockTenantModuleBootstrapper(SDL) {
            field("Query" to "boo") {
                resolver {
                    fn { _, _, _, _, _ ->
                        object : EngineObjectData {
                            override val graphQLObjectType = booType

                            override suspend fun fetch(selection: String): Any? {
                                if (selection == "value") {
                                    throw RuntimeException("FakeBoo.value is throwing")
                                }
                                return null
                            }
                        }
                    }
                }
            }
            field("Boo" to "value") {
                checker {
                    fn { _, _ -> throw RuntimeException("permission denied") }
                }
            }
        }.runFeatureTest {
            val result = viaduct.runQuery("{ boo { value } }")
            assertEquals(mapOf("boo" to mapOf("value" to null)), result.getData())
            // datafetcher error takes priority
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("boo", "value"), error.path)
            assertTrue(error.message.contains("FakeBoo.value is throwing"))
        }
    }

    @Test
    fun `objectValueFragment field - field and checker successful`() {
        MockTenantModuleBootstrapper(SDL) {
            fieldWithValue("Query" to "string2", "2nd")
            field("Query" to "string1") {
                resolver {
                    objectSelections("string2")
                    fn { _, obj, _, _, _ ->
                        val value = obj.fetch("string2")
                        "1st & $value"
                    }
                }
            }
            field("Query" to "string2") {
                checker {
                    fn { _, _ -> /* access granted */ }
                }
            }
        }.runFeatureTest {
            viaduct.runQuery("{ string1 }")
                .assertJson("{data: {string1: \"1st & 2nd\"}}")
        }
    }

    @Test
    fun `objectValueFragment field - field successful, checker throws`() {
        MockTenantModuleBootstrapper(SDL) {
            fieldWithValue("Query" to "string2", "2nd")
            field("Query" to "string1") {
                resolver {
                    objectSelections("string2")
                    fn { _, obj, _, _, _ ->
                        val value = obj.fetch("string2")
                        "1st & $value"
                    }
                }
            }
            field("Query" to "string2") {
                checker {
                    fn { _, _ -> throw RuntimeException("string2 permission denied") }
                }
            }
        }.runFeatureTest {
            val result = viaduct.runQuery("{ string1 }")
            assertEquals(mapOf("string1" to null), result.getData())
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("string1"), error.path)
            assertTrue(error.message.contains("string2 permission denied"))
        }
    }

    @Test
    fun `objectValueFragment field - field throws, checker throws`() {
        MockTenantModuleBootstrapper(SDL) {
            field("Query" to "string2") {
                resolver {
                    fn { _, _, _, _, _ -> throw RuntimeException("string2 resolver throws") }
                }
                checker {
                    fn { _, _ -> throw RuntimeException("string2 permission denied") }
                }
            }
            field("Query" to "string1") {
                resolver {
                    objectSelections("string2")
                    fn { _, obj, _, _, _ ->
                        val value = obj.fetch("string2")
                        "1st & $value"
                    }
                }
            }
        }.runFeatureTest {
            val result = viaduct.runQuery("{ string1 }")
            assertEquals(mapOf("string1" to null), result.getData())
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("string1"), error.path)
            // datafetcher error takes priority
            assertTrue(error.message.contains("string2 resolver throws"))
        }
    }

    @Test
    fun `objectValueFragment field - field throws, checker successful`() {
        MockTenantModuleBootstrapper(SDL) {
            field("Query" to "string2") {
                resolver {
                    fn { _, _, _, _, _ -> throw RuntimeException("string2 resolver throws") }
                }
                checker {
                    fn { _, _ -> /* access granted */ }
                }
            }
            field("Query" to "string1") {
                resolver {
                    objectSelections("string2")
                    fn { _, obj, _, _, _ ->
                        val value = obj.fetch("string2")
                        "1st & $value"
                    }
                }
            }
        }.runFeatureTest {
            val result = viaduct.runQuery("{ string1 }")
            assertEquals(mapOf("string1" to null), result.getData())
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("string1"), error.path)
            assertTrue(error.message.contains("string2 resolver throws"))
        }
    }

    @Test
    fun `checker with rss - access checks skipped`() {
        MockTenantModuleBootstrapper(SDL) {
            fieldWithValue("Query" to "string1", "foo")
            fieldWithValue("Query" to "string2", "bar")
            field("Query" to "string1") {
                checker {
                    objectSelections("key", "fragment _ on Query { string2 }")
                    fn { _, objectDataMap ->
                        if (objectDataMap["key"]?.fetch("string2") == "bar") {
                            // Verifies that the access check for string2 shouldn't be applied
                            // during fetch, otherwise the error would be "string2 checker failed"
                            throw RuntimeException("this should get thrown")
                        }
                    }
                }
            }
            field("Query" to "string2") {
                checker {
                    fn { _, _ -> throw RuntimeException("string2 checker failed") }
                }
            }
        }.runFeatureTest {
            val result = viaduct.runQuery("{ string1 }")
            assertEquals(mapOf("string1" to null), result.getData())
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("string1"), error.path)
            assertTrue(error.message.contains("this should get thrown"))
        }
    }

    @Test
    fun `mutation field with checker`() {
        var mutationResolverRan = false

        MockTenantModuleBootstrapper(SDL) {
            field("Mutation" to "string1") {
                resolver {
                    fn { _, _, _, _, _ ->
                        mutationResolverRan = true
                        "foo"
                    }
                }
                checker {
                    fn { _, _ -> throw RuntimeException("string1 checker failed") }
                }
            }
        }.runFeatureTest {
            val result = viaduct.runQuery("mutation { string1 }")
            assertEquals(mapOf("string1" to null), result.getData())
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("string1"), error.path)
            assertTrue(error.message.contains("string1 checker failed"))
        }

        assertFalse(mutationResolverRan)
    }

    @Test
    fun `field and type checks fail`() {
        MockTenantModuleBootstrapper(SDL) {
            field("Query" to "baz") {
                resolver {
                    fn { _, _, _, _, ctx -> ctx.createNodeEngineObjectData("1", bazType) }
                }
                checker {
                    fn { _, _ -> throw RuntimeException("field checker failed") }
                }
            }
            type("Baz") {
                nodeUnbatchedExecutor { id, _, _ ->
                    MockEngineObjectData(bazType, mapOf("id" to id, "x" to id.toInt(), "y" to id))
                }
                checker {
                    fn { _, _ -> throw RuntimeException("type checker failed") }
                }
            }
        }.runFeatureTest {
            val result = viaduct.runQuery("{ baz { id } }")
            assertEquals(mapOf("baz" to null), result.getData())
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("baz"), error.path)
            assertTrue(error.message.contains("field checker failed"))
        }
    }

    @Test
    fun `field check succeeds, type check fails`() {
        MockTenantModuleBootstrapper(SDL) {
            field("Query" to "baz") {
                resolver {
                    fn { _, _, _, _, ctx -> ctx.createNodeEngineObjectData("1", bazType) }
                }
                checker {
                    fn { _, _ -> /* access granted */ }
                }
            }
            type("Baz") {
                nodeUnbatchedExecutor { id, _, _ ->
                    MockEngineObjectData(bazType, mapOf("id" to id, "x" to id.toInt(), "y" to id))
                }
                checker {
                    fn { _, _ -> throw RuntimeException("type checker failed") }
                }
            }
        }.runFeatureTest {
            val result = viaduct.runQuery("{ baz { id } }")
            assertEquals(mapOf("baz" to null), result.getData())
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("baz"), error.path)
            assertTrue(error.message.contains("type checker failed"))
        }
    }

    @Test
    fun `no field check, type check fails`() {
        MockTenantModuleBootstrapper(SDL) {
            field("Query" to "baz") {
                resolver {
                    fn { _, _, _, _, ctx -> ctx.createNodeEngineObjectData("1", bazType) }
                }
            }
            type("Baz") {
                nodeUnbatchedExecutor { id, _, _ ->
                    MockEngineObjectData(bazType, mapOf("id" to id, "x" to id.toInt(), "y" to id))
                }
                checker {
                    fn { _, _ -> throw RuntimeException("type checker failed") }
                }
            }
        }.runFeatureTest {
            val result = viaduct.runQuery("{ baz { id } }")
            assertEquals(mapOf("baz" to null), result.getData())
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("baz"), error.path)
            assertTrue(error.message.contains("type checker failed"))
        }
    }

    @Test
    fun `type check on interface field`() {
        MockTenantModuleBootstrapper(SDL) {
            field("Query" to "node") {
                resolver {
                    fn { _, _, _, _, ctx -> ctx.createNodeEngineObjectData("1", bazType) }
                }
            }
            type("Baz") {
                nodeUnbatchedExecutor { id, _, _ ->
                    MockEngineObjectData(bazType, mapOf("id" to id, "x" to id.toInt(), "y" to id))
                }
                checker {
                    fn { _, _ -> throw RuntimeException("type checker failed") }
                }
            }
        }.runFeatureTest {
            val result = viaduct.runQuery("{ node { id } }")
            assertEquals(mapOf("node" to null), result.getData())
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("node"), error.path)
            assertTrue(error.message.contains("type checker failed"))
        }
    }

    @Test
    fun `type check with rss`() {
        MockTenantModuleBootstrapper(SDL) {
            field("Query" to "baz") {
                resolver {
                    fn { _, _, _, _, ctx -> ctx.createNodeEngineObjectData("1", bazType) }
                }
            }
            field("Baz" to "y") {
                resolver {
                    objectSelections("x")
                    fn { _, obj, _, _, _ -> obj.fetch("x").toString() }
                }
            }
            type("Baz") {
                nodeUnbatchedExecutor { id, _, _ ->
                    MockEngineObjectData(bazType, mapOf("id" to id, "x" to id.toInt(), "y" to id))
                }
                checker {
                    objectSelections("key", "fragment _ on Baz { y }")
                    fn { _, objectDataMap ->
                        if (objectDataMap["key"]!!.fetch("y") == "1") {
                            // Verifies that this got the correct value for a dependent field
                            // without a circular dep on the type checker
                            throw RuntimeException("this should get thrown")
                        }
                    }
                }
            }
        }.runFeatureTest {
            // TODO: remove "y" from the query here once we add type checker RSSs to the query plan
            val result = viaduct.runQuery("{ baz { id y } }")
            assertEquals(mapOf("baz" to null), result.getData())
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("baz"), error.path)
            assertTrue(error.message.contains("this should get thrown"))
        }
    }

    @Test
    fun `type checks for list of objects`() {
        MockTenantModuleBootstrapper(SDL) {
            field("Query" to "bazList") {
                resolver {
                    fn { _, _, _, _, ctx ->
                        listOf(
                            ctx.createNodeEngineObjectData("1", bazType),
                            ctx.createNodeEngineObjectData("2", bazType),
                            ctx.createNodeEngineObjectData("3", bazType),
                        )
                    }
                }
            }
            field("Baz" to "y") {
                resolver {
                    objectSelections("x")
                    fn { _, obj, _, _, _ ->
                        val x = obj.fetch("x")
                        x.toString()
                    }
                }
            }
            type("Baz") {
                nodeUnbatchedExecutor { id, _, _ ->
                    MockEngineObjectData(bazType, mapOf("id" to id, "x" to id.toInt(), "y" to id))
                }
                checker {
                    objectSelections("key", "fragment _ on Baz { y }")
                    fn { _, objectDataMap ->
                        val eod = objectDataMap["key"]!!
                        val y = eod.fetch("y")
                        if (y == "2") {
                            throw RuntimeException("permission denied for baz with internal ID 2")
                        }
                    }
                }
            }
        }.runFeatureTest {
            // TODO: remove "y" from the query here once we add type checker RSSs to the query plan
            val result = viaduct.runQuery("{ bazList { id y } }")
            val expectedData = mapOf(
                "bazList" to listOf(
                    mapOf("id" to "1", "y" to "1"),
                    null,
                    mapOf("id" to "3", "y" to "3")
                )
            )
            assertEquals(expectedData, result.getData())
            assertEquals(1, result.errors.size)
            val error = result.errors[0]
            assertEquals(listOf("bazList", 1), error.path)
            assertTrue(error.message.contains("permission denied for baz with internal ID 2"))
        }
    }
}
