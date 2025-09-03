@file:Suppress("unused", "ClassName")

package viaduct.tenant.runtime.tutorials.simplebatchresolvers

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals as kotlinAssertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import viaduct.api.FieldValue
import viaduct.api.Resolver
import viaduct.graphql.test.assertEquals
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase
import viaduct.tenant.runtime.tutorials.simplebatchresolvers.resolverbases.QueryResolvers
import viaduct.tenant.runtime.tutorials.simplebatchresolvers.resolverbases.UserResolvers

/**
 * Demonstrates Viaduct's Batch Resolver feature for efficient data loading.
 *
 * Batch Resolvers solve the N+1 query problem by collecting multiple field requests
 * and resolving them in a single batch operation. Instead of making separate database
 * calls for each user's department, one call can fetch departments for all users.
 *
 * When multiple objects need the same type of data, Viaduct automatically groups
 * the requests and calls batchResolve() once with all contexts, similar to GraphQL DataLoaders.
 */
class SimpleBatchResolverFeatureAppTest : FeatureAppTestBase() {
    override var sdl = """
        | #START_SCHEMA
        | directive @resolver on FIELD_DEFINITION | OBJECT
        |
        | type Query {
        |   users: [User!]! @resolver
        |   user(id: String!): User @resolver
        | }
        |
        | type User {
        |   id: String!
        |   name: String!
        |   department: String @resolver
        | }
        | #END_SCHEMA
    """.trimMargin()

    companion object {
        // Track batchResolve calls to prove batching efficiency
        val batchResolveCalls = ConcurrentLinkedQueue<Int>()

        fun resetCallTracking() {
            batchResolveCalls.clear()
        }

        fun getTotalBatchResolveCalls(): Int {
            return batchResolveCalls.size
        }
    }

    @BeforeEach
    fun setUp() {
        resetCallTracking()
    }

    /**
     * Returns a list of users for the query. This creates multiple User objects
     * that will each need their department field resolved.
     */
    @Resolver
    class Query_UsersResolver : QueryResolvers.Users() {
        override suspend fun resolve(ctx: Context): List<User> {
            // Static user data - in reality this would come from a database
            return listOf(
                User.Builder(ctx)
                    .id("user-1")
                    .name("Alice Johnson")
                    .build(),
                User.Builder(ctx)
                    .id("user-2")
                    .name("Bob Smith")
                    .build(),
                User.Builder(ctx)
                    .id("user-3")
                    .name("Carol Williams")
                    .build()
            )
        }
    }

    /**
     * Returns a single user by ID
     */
    @Resolver
    class Query_UserResolver : QueryResolvers.User() {
        override suspend fun resolve(ctx: Context): User? {
            val userId = ctx.arguments.id

            // Mock user lookup - in reality this would be a database call
            return when (userId) {
                "user-1" -> User.Builder(ctx).id("user-1").name("Alice Johnson").build()
                "user-2" -> User.Builder(ctx).id("user-2").name("Bob Smith").build()
                "user-3" -> User.Builder(ctx).id("user-3").name("Carol Williams").build()
                else -> null
            }
        }
    }

    /**
     * Batch Resolver for user departments. Instead of making separate database calls
     * for each user's department, this resolver gets all user IDs at once and can
     * make a single optimized query to fetch all departments.
     *
     * batchResolve() receives all contexts that need department data and returns
     * a corresponding list of FieldValue results in the same order.
     */
    @Resolver(
        objectValueFragment = "fragment _ on User { id }"
    )
    class User_DepartmentResolver : UserResolvers.Department() {
        override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<String>> {
            // Extract all user IDs from the batch
            val userIds = contexts.map { ctx -> ctx.objectValue.getId() }

            // Track this batchResolve call - record the batch size
            batchResolveCalls.add(userIds.size)

            // Simulate a single database call that fetches departments for all users
            // In reality: SELECT user_id, department FROM user_departments WHERE user_id IN (...)
            val departmentData = fetchDepartmentsForUsers(userIds)

            // Return results in the same order as input contexts
            return contexts.map { ctx ->
                val userId = ctx.objectValue.getId()
                val department = departmentData[userId] ?: "Unknown"
                FieldValue.ofValue(department)
            }
        }

        // Simulates a single database query for all user departments
        private fun fetchDepartmentsForUsers(userIds: List<String>): Map<String, String> {
            // Mock department mapping - in reality this would be a database call
            return mapOf(
                "user-1" to "Engineering",
                "user-2" to "Marketing",
                "user-3" to "Engineering"
            ).filter { it.key in userIds }
        }
    }

    @Test
    fun `Batch resolver correctly loads departments for multiple users with single database call`() {
        resetCallTracking()

        execute(
            query = """
                query {
                    users {
                        id
                        name
                        department
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "users" to arrayOf(
                    {
                        "id" to "user-1"
                        "name" to "Alice Johnson"
                        "department" to "Engineering" // Loaded via batch resolver
                    },
                    {
                        "id" to "user-2"
                        "name" to "Bob Smith"
                        "department" to "Marketing" // Same batch call
                    },
                    {
                        "id" to "user-3"
                        "name" to "Carol Williams"
                        "department" to "Engineering" // Same batch call
                    }
                )
            }
        }

        // Assert batching efficiency: only 1 batchResolve call for all 3 users
        kotlinAssertEquals(1, getTotalBatchResolveCalls(), "Expected exactly 1 batchResolve call for batch loading")
        kotlinAssertEquals(3, batchResolveCalls.first(), "Expected batch size of 3 users in single call")
    }

    @Test
    fun `Single user query triggers batch resolver with size 1`() {
        resetCallTracking()

        execute(
            query = """
                query {
                    user(id: "user-1") {
                        id
                        name
                        department
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "user" to {
                    "id" to "user-1"
                    "name" to "Alice Johnson"
                    "department" to "Engineering"
                }
            }
        }

        // Assert: single user still uses batch resolver (batch size 1)
        kotlinAssertEquals(1, getTotalBatchResolveCalls(), "Expected exactly 1 batchResolve call for single user")
        kotlinAssertEquals(1, batchResolveCalls.first(), "Expected batch size of 1 user in single call")
    }

    @Test
    fun `Mixed query with users list and single user demonstrates batching efficiency`() {
        resetCallTracking()

        execute(
            query = """
                query {
                    users {
                        id
                        name
                        department
                    }
                    singleUser: user(id: "user-2") {
                        id
                        name
                        department
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "users" to arrayOf(
                    {
                        "id" to "user-1"
                        "name" to "Alice Johnson"
                        "department" to "Engineering"
                    },
                    {
                        "id" to "user-2"
                        "name" to "Bob Smith"
                        "department" to "Marketing"
                    },
                    {
                        "id" to "user-3"
                        "name" to "Carol Williams"
                        "department" to "Engineering"
                    }
                )
                "singleUser" to {
                    "id" to "user-2"
                    "name" to "Bob Smith"
                    "department" to "Marketing"
                }
            }
        }

        // Assert: Viaduct should batch all department requests together
        // 4 total department requests (3 from users + 1 from singleUser) = 1 batch call
        kotlinAssertEquals(1, getTotalBatchResolveCalls(), "Expected exactly 1 batchResolve call for all department requests")
        kotlinAssertEquals(4, batchResolveCalls.first(), "Expected batch size of 4 users (3 + 1 duplicate)")
    }

    @Test
    fun `Conditional department requests still batch together`() {
        resetCallTracking()

        execute(
            query = """
                query {
                    users {
                        id
                        name
                        department @include(if: true)
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "users" to arrayOf(
                    {
                        "id" to "user-1"
                        "name" to "Alice Johnson"
                        "department" to "Engineering"
                    },
                    {
                        "id" to "user-2"
                        "name" to "Bob Smith"
                        "department" to "Marketing"
                    },
                    {
                        "id" to "user-3"
                        "name" to "Carol Williams"
                        "department" to "Engineering"
                    }
                )
            }
        }

        // Assert: conditional inclusion still results in batching
        kotlinAssertEquals(1, getTotalBatchResolveCalls(), "Expected exactly 1 batchResolve call for conditional department requests")
        kotlinAssertEquals(3, batchResolveCalls.first(), "Expected batch size of 3 users")
    }

    @Test
    fun `Query without department field does not trigger batch resolver`() {
        resetCallTracking()

        execute(
            query = """
                query {
                    users {
                        id
                        name
                        # No department field requested
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "users" to arrayOf(
                    {
                        "id" to "user-1"
                        "name" to "Alice Johnson"
                    },
                    {
                        "id" to "user-2"
                        "name" to "Bob Smith"
                    },
                    {
                        "id" to "user-3"
                        "name" to "Carol Williams"
                    }
                )
            }
        }

        // Assert: no department requests = no batchResolve calls
        kotlinAssertEquals(0, getTotalBatchResolveCalls(), "Expected no batchResolve calls when department field not requested")
    }
}
