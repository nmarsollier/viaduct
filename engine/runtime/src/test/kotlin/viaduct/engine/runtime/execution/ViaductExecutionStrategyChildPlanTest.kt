package viaduct.engine.runtime.execution

import graphql.schema.DataFetcher
import graphql.schema.TypeResolver
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import viaduct.dataloader.NextTickDispatcher
import viaduct.engine.api.mocks.MockRequiredSelectionSetRegistry
import viaduct.engine.runtime.execution.ExecutionTestHelpers.executeViaductModernGraphQL
import viaduct.engine.runtime.execution.ExecutionTestHelpers.runExecutionTest
import viaduct.service.api.spi.FlagManager

/**
 * Tests for ViaductExecutionStrategy child plan functionality.
 *
 * This test class specifically covers child plan execution scenarios that occur
 * when Required Selection Sets (RSS) are configured for fields. Child plans are
 * sub-executions that fetch additional data needed for authorization or field
 * resolution logic.
 *
 * Test scenarios include:
 * - Child plans maintaining correct execution paths (not inheriting parent field paths)
 * - Query-type vs. Object-type child plan execution contexts
 * - Nested object types with RSS at multiple levels
 * - List fields with RSS for each item
 * - Interface and union types with RSS
 * - Mixed Query and Object type child plans in the same execution
 *
 * These tests ensure that the fix for child plan path handling (commit 589665aee1c7d)
 * works correctly across various GraphQL schema configurations.
 *
 * For general execution strategy tests, see ViaductExecutionStrategyTest.
 * For modern vs. classic strategy comparisons, see ViaductExecutionStrategyModernTest.
 */
@ExperimentalCoroutinesApi
class ViaductExecutionStrategyChildPlanTest {
    private val nextTickDispatcher = NextTickDispatcher(flagManager = FlagManager.disabled)

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `child plans execute with fresh root path and correct object type`() {
        runExecutionTest {
            withContext(nextTickDispatcher) {
                val childPlanExecutionStepInfos = ConcurrentLinkedQueue<graphql.execution.ExecutionStepInfo>()

                val sdl = """
                    type Query {
                        testEntity: TestEntity
                    }

                    type TestEntity {
                        id: ID!
                        name: String
                        details: String
                        childPlanField: String
                    }
                """

                val query = """
                    {
                        testEntity {
                            id
                            name
                            details
                            ...Foo
                        }
                    }
                    fragment Foo on TestEntity {
                        id
                    }
                """

                val resolvers = mapOf(
                    "Query" to mapOf(
                        "testEntity" to DataFetcher { _ ->
                            mapOf("id" to "123", "name" to "Test Entity")
                        }
                    ),
                    "TestEntity" to mapOf(
                        "id" to DataFetcher { env ->
                            env.getSource<Map<String, Any>>()!!["id"]
                        },
                        "name" to DataFetcher { env ->
                            env.getSource<Map<String, Any>>()!!["name"]
                        },
                        "details" to DataFetcher { _ ->
                            // childPlanExecutionStepInfos.add(env.executionStepInfo)
                            "Entity details"
                        },
                        "childPlanField" to DataFetcher { env ->
                            childPlanExecutionStepInfos.add(env.executionStepInfo)
                            null
                        }
                    )
                )

                val requiredSelectionSetRegistry = MockRequiredSelectionSetRegistry.builder()
                    .fieldResolverEntry("TestEntity" to "details", "fragment Main on TestEntity { childPlanField ...Bar } fragment Bar on TestEntity { id }")
                    .build()

                val executionResult = executeViaductModernGraphQL(
                    sdl = sdl,
                    resolvers = resolvers,
                    query = query,
                    requiredSelectionSetRegistry = requiredSelectionSetRegistry
                )

                val data = executionResult.getData<Map<String, Any?>>()
                assertNotNull(data)
                val testEntity = data["testEntity"] as Map<String, Any?>
                assertEquals("123", testEntity["id"])
                assertEquals("Test Entity", testEntity["name"])
                assertEquals("Entity details", testEntity["details"])
                assertNull(testEntity["childPlanField"])

                assertTrue(childPlanExecutionStepInfos.isNotEmpty(), "Expected child plan to be executed")
                val childStepInfo = childPlanExecutionStepInfos.first()
                assertEquals(listOf("testEntity", "childPlanField"), childStepInfo.path.toList())
            }
        }
    }

    @Test
    fun `child plans for Query type use correct execution context`() {
        runExecutionTest {
            withContext(nextTickDispatcher) {
                var childPlanObjectType: String? = null

                val sdl = """
                    type Query {
                        specialField: String
                        helperField: String
                    }
                """

                val query = """
                    {
                        specialField
                    }
                """

                val resolvers = mapOf(
                    "Query" to mapOf(
                        "specialField" to DataFetcher { env ->
                            childPlanObjectType = env.executionStepInfo.objectType?.name
                            "Special value"
                        },
                        "helperField" to DataFetcher { "Helper value" }
                    )
                )

                // Create a Query-level required selection set
                val requiredSelectionSetRegistry = MockRequiredSelectionSetRegistry.builder()
                    .fieldResolverEntry("Query" to "specialField", "__typename")
                    .build()

                val executionResult = executeViaductModernGraphQL(
                    sdl = sdl,
                    resolvers = resolvers,
                    query = query,
                    requiredSelectionSetRegistry = requiredSelectionSetRegistry
                )

                val data = executionResult.getData<Map<String, Any?>>()
                assertEquals("Special value", data["specialField"])
                assertEquals("Query", childPlanObjectType, "Expected Query type for Query-level child plan")
            }
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `child plans for object field checkers use fresh root path - reproduces original bug`() {
        runExecutionTest {
            withContext(nextTickDispatcher) {
                val capturedPaths = ConcurrentLinkedQueue<String>()

                val sdl = """
                    type Query {
                        node: TestNode
                    }

                    type TestNode {
                        id: ID!
                        restrictedField: Details
                    }

                    type Details {
                        content: String
                    }
                """

                val query = """
                    {
                        node {
                            restrictedField {
                                content
                            }
                        }
                    }
                """

                val resolvers = mapOf(
                    "Query" to mapOf(
                        "node" to DataFetcher { _ ->
                            mapOf("id" to "node-123")
                        }
                    ),
                    "TestNode" to mapOf(
                        "id" to DataFetcher { env ->
                            val path = env.executionStepInfo.path.toString()
                            capturedPaths.add(path)
                            env.getSource<Map<String, Any>>()!!["id"]
                        },
                        "restrictedField" to DataFetcher { _ ->
                            mapOf("content" to "Protected content")
                        }
                    ),
                    "Details" to mapOf(
                        "content" to DataFetcher { env ->
                            env.getSource<Map<String, Any>>()!!["content"]
                        }
                    )
                )

                val requiredSelectionSetRegistry = MockRequiredSelectionSetRegistry.builder()
                    .fieldResolverEntry("TestNode" to "restrictedField", "id")
                    .build()

                val executionResult = executeViaductModernGraphQL(
                    sdl = sdl,
                    resolvers = resolvers,
                    query = query,
                    requiredSelectionSetRegistry = requiredSelectionSetRegistry
                )

                val data = executionResult.getData<Map<String, Any?>>()
                assertNotNull(data)
                val node = data["node"] as Map<String, Any?>
                val restrictedField = node["restrictedField"] as Map<String, Any?>
                assertEquals("Protected content", restrictedField["content"])

                assertTrue(capturedPaths.isNotEmpty(), "Expected id to be fetched for checker RSS")
                val idPath = capturedPaths.first()
                assertEquals(
                    "/node/id",
                    idPath,
                    "Child plan should execute with fresh root path. " +
                        "Got '$idPath' but expected '/node/id'. " +
                        "This indicates the child plan incorrectly inherited the field's path."
                )
            }
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `mixed child plans - Query and Object types in same execution`() {
        runExecutionTest {
            withContext(nextTickDispatcher) {
                val capturedPaths = ConcurrentLinkedQueue<Pair<String, String>>()

                val sdl = """
                    type Query {
                        item: Item
                        globalConfig: Config
                    }

                    type Item {
                        id: ID!
                        restricted: String
                    }

                    type Config {
                        value: String
                    }
                """

                val query = """
                    {
                        item {
                            restricted
                        }
                    }
                """

                val resolvers = mapOf(
                    "Query" to mapOf(
                        "item" to DataFetcher { _ ->
                            mapOf("id" to "item-123")
                        },
                        "globalConfig" to DataFetcher { env ->
                            capturedPaths.add("globalConfig" to env.executionStepInfo.path.toString())
                            mapOf("value" to "config-value")
                        }
                    ),
                    "Item" to mapOf(
                        "id" to DataFetcher { env ->
                            capturedPaths.add("item.id" to env.executionStepInfo.path.toString())
                            env.getSource<Map<String, Any>>()!!["id"]
                        },
                        "restricted" to DataFetcher { _ ->
                            "restricted-value"
                        }
                    ),
                    "Config" to mapOf(
                        "value" to DataFetcher { env ->
                            env.getSource<Map<String, Any>>()!!["value"]
                        }
                    )
                )

                val requiredSelectionSetRegistry = MockRequiredSelectionSetRegistry.builder()
                    .fieldResolverEntry("Item" to "restricted", "id")
                    .fieldResolverEntryForType("Query", "Item" to "restricted", "globalConfig { value }")
                    .build()

                val executionResult = executeViaductModernGraphQL(
                    sdl = sdl,
                    resolvers = resolvers,
                    query = query,
                    requiredSelectionSetRegistry = requiredSelectionSetRegistry
                )

                val data = executionResult.getData<Map<String, Any?>>()
                assertNotNull(data)
                val item = data["item"] as Map<String, Any?>
                assertEquals("restricted-value", item["restricted"])

                assertTrue(capturedPaths.size >= 2, "Expected both Object and Query child plans to execute")

                val itemPath = capturedPaths.find { it.first == "item.id" }?.second
                assertEquals("/item/id", itemPath, "Object-type child plan should use parent object path")

                val queryPath = capturedPaths.find { it.first == "globalConfig" }?.second
                assertEquals("/globalConfig", queryPath, "Query-type child plan should use root path")
            }
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `nested object types with RSS at multiple levels maintain correct paths`() {
        runExecutionTest {
            withContext(nextTickDispatcher) {
                val capturedPaths = ConcurrentLinkedQueue<Pair<String, String>>()

                val sdl = """
                    type Query {
                        root: Level1
                    }

                    type Level1 {
                        id: ID!
                        level2: Level2
                    }

                    type Level2 {
                        id: ID!
                        level3: Level3
                    }

                    type Level3 {
                        id: ID!
                        data: String
                    }
                """

                val query = """
                    {
                        root {
                            level2 {
                                level3 {
                                    data
                                }
                            }
                        }
                    }
                """

                val resolvers = mapOf(
                    "Query" to mapOf(
                        "root" to DataFetcher { _ ->
                            mapOf("id" to "l1-id")
                        }
                    ),
                    "Level1" to mapOf(
                        "id" to DataFetcher { env ->
                            capturedPaths.add("Level1.id" to env.executionStepInfo.path.toString())
                            env.getSource<Map<String, Any>>()!!["id"]
                        },
                        "level2" to DataFetcher { _ ->
                            mapOf("id" to "l2-id")
                        }
                    ),
                    "Level2" to mapOf(
                        "id" to DataFetcher { env ->
                            capturedPaths.add("Level2.id" to env.executionStepInfo.path.toString())
                            env.getSource<Map<String, Any>>()!!["id"]
                        },
                        "level3" to DataFetcher { _ ->
                            mapOf("id" to "l3-id")
                        }
                    ),
                    "Level3" to mapOf(
                        "id" to DataFetcher { env ->
                            capturedPaths.add("Level3.id" to env.executionStepInfo.path.toString())
                            env.getSource<Map<String, Any>>()!!["id"]
                        },
                        "data" to DataFetcher { _ ->
                            "final-data"
                        }
                    )
                )

                val requiredSelectionSetRegistry = MockRequiredSelectionSetRegistry.builder()
                    .fieldResolverEntry("Level1" to "level2", "id")
                    .fieldResolverEntry("Level2" to "level3", "id")
                    .fieldResolverEntry("Level3" to "data", "id")
                    .build()

                val executionResult = executeViaductModernGraphQL(
                    sdl = sdl,
                    resolvers = resolvers,
                    query = query,
                    requiredSelectionSetRegistry = requiredSelectionSetRegistry
                )

                val data = executionResult.getData<Map<String, Any?>>()
                assertNotNull(data)
                val root = data["root"] as Map<String, Any?>
                val level2 = root["level2"] as Map<String, Any?>
                val level3 = level2["level3"] as Map<String, Any?>
                assertEquals("final-data", level3["data"])

                assertTrue(capturedPaths.size >= 3, "Expected RSS at all three levels")

                val l1Path = capturedPaths.find { it.first == "Level1.id" }?.second
                assertEquals("/root/id", l1Path, "Level1 RSS should use parent object path")

                val l2Path = capturedPaths.find { it.first == "Level2.id" }?.second
                assertEquals("/root/level2/id", l2Path, "Level2 RSS should use parent object path")

                val l3Path = capturedPaths.find { it.first == "Level3.id" }?.second
                assertEquals("/root/level2/level3/id", l3Path, "Level3 RSS should use parent object path")
            }
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `list fields with RSS execute child plans with correct paths for each item`() {
        runExecutionTest {
            withContext(nextTickDispatcher) {
                val capturedPaths = ConcurrentLinkedQueue<Pair<String, String>>()

                val sdl = """
                    type Query {
                        items: [ListItem]
                    }

                    type ListItem {
                        id: ID!
                        restricted: String
                    }
                """

                val query = """
                    {
                        items {
                            restricted
                        }
                    }
                """

                val resolvers = mapOf(
                    "Query" to mapOf(
                        "items" to DataFetcher { _ ->
                            listOf(
                                mapOf("id" to "item-1"),
                                mapOf("id" to "item-2"),
                                mapOf("id" to "item-3")
                            )
                        }
                    ),
                    "ListItem" to mapOf(
                        "id" to DataFetcher { env ->
                            val id = env.getSource<Map<String, Any>>()!!["id"]
                            val path = env.executionStepInfo.path.toString()
                            capturedPaths.add(id.toString() to path)
                            id
                        },
                        "restricted" to DataFetcher { env ->
                            val id = env.getSource<Map<String, Any>>()!!["id"]
                            "restricted-$id"
                        }
                    )
                )

                val requiredSelectionSetRegistry = MockRequiredSelectionSetRegistry.builder()
                    .fieldResolverEntry("ListItem" to "restricted", "id")
                    .build()

                val executionResult = executeViaductModernGraphQL(
                    sdl = sdl,
                    resolvers = resolvers,
                    query = query,
                    requiredSelectionSetRegistry = requiredSelectionSetRegistry
                )

                val data = executionResult.getData<Map<String, Any?>>()
                assertNotNull(data)
                val items = data["items"] as List<Map<String, Any?>>
                assertEquals(3, items.size)
                assertEquals("restricted-item-1", items[0]["restricted"])
                assertEquals("restricted-item-2", items[1]["restricted"])
                assertEquals("restricted-item-3", items[2]["restricted"])

                assertEquals(3, capturedPaths.size, "Expected 3 captured paths, got ${capturedPaths.size}: $capturedPaths")

                val item1Path = capturedPaths.find { it.first == "item-1" }?.second
                assertEquals("/items[0]/id", item1Path, "First list item RSS should have correct index path")

                val item2Path = capturedPaths.find { it.first == "item-2" }?.second
                assertEquals("/items[1]/id", item2Path, "Second list item RSS should have correct index path")

                val item3Path = capturedPaths.find { it.first == "item-3" }?.second
                assertEquals("/items[2]/id", item3Path, "Third list item RSS should have correct index path")
            }
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `interface types with RSS use correct parent type for child plans`() {
        runExecutionTest {
            withContext(nextTickDispatcher) {
                val capturedTypes = ConcurrentLinkedQueue<Pair<String, String?>>()

                val sdl = """
                    type Query {
                        entity: Entity
                    }

                    interface Entity {
                        id: ID!
                    }

                    type User implements Entity {
                        id: ID!
                        name: String
                        restricted: String
                    }

                    type Admin implements Entity {
                        id: ID!
                        role: String
                        restricted: String
                    }
                """

                val query = """
                    {
                        entity {
                            ... on User {
                                restricted
                            }
                            ... on Admin {
                                restricted
                            }
                        }
                    }
                """

                val resolvers = mapOf(
                    "Query" to mapOf(
                        "entity" to DataFetcher { _ ->
                            mapOf("id" to "user-123", "__typename" to "User")
                        }
                    ),
                    "User" to mapOf(
                        "id" to DataFetcher { env ->
                            val objectType = env.executionStepInfo.objectType?.name
                            capturedTypes.add("User.id" to objectType)
                            env.getSource<Map<String, Any>>()!!["id"]
                        },
                        "name" to DataFetcher { "John" },
                        "restricted" to DataFetcher { "user-restricted" }
                    ),
                    "Admin" to mapOf(
                        "id" to DataFetcher { env ->
                            val objectType = env.executionStepInfo.objectType?.name
                            capturedTypes.add("Admin.id" to objectType)
                            env.getSource<Map<String, Any>>()!!["id"]
                        },
                        "role" to DataFetcher { "super" },
                        "restricted" to DataFetcher { "admin-restricted" }
                    )
                )

                val requiredSelectionSetRegistry = MockRequiredSelectionSetRegistry.builder()
                    .fieldResolverEntry("User" to "restricted", "id")
                    .fieldResolverEntry("Admin" to "restricted", "id")
                    .build()

                val typeResolvers = mapOf(
                    "Entity" to TypeResolver { env ->
                        val obj = env.getObject<Map<String, Any>>()
                        val typename = obj["__typename"] as? String
                        env.schema.getObjectType(typename ?: "User")
                    }
                )

                val executionResult = executeViaductModernGraphQL(
                    sdl = sdl,
                    resolvers = resolvers,
                    query = query,
                    typeResolvers = typeResolvers,
                    requiredSelectionSetRegistry = requiredSelectionSetRegistry
                )

                val data = executionResult.getData<Map<String, Any?>>()
                assertNotNull(data)
                val entity = data["entity"] as Map<String, Any?>
                assertEquals("user-restricted", entity["restricted"])

                assertTrue(capturedTypes.isNotEmpty(), "Expected RSS to execute for concrete type")

                val userType = capturedTypes.find { it.first == "User.id" }?.second
                assertEquals("User", userType, "Child plan should use concrete User type, not Entity interface")
            }
        }
    }
}
