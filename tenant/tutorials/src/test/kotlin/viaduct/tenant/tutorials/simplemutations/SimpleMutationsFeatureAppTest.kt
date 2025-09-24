@file:Suppress("unused", "ClassName")

package viaduct.tenant.tutorials.simplemutations

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.graphql.test.assertEquals
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase
import viaduct.tenant.tutorials.simplemutations.resolverbases.MutationResolvers
import viaduct.tenant.tutorials.simplemutations.resolverbases.QueryResolvers

/**
 * Demonstrates basic GraphQL mutations in Viaduct with best practices.
 *
 * This example shows the fundamental mutation operations:
 * create, update, and query using thread-safe data structures
 * and proper ID extraction from results.
 */
class SimpleMutationsFeatureAppTest : FeatureAppTestBase() {
    override var sdl =
        """
        | #START_SCHEMA
        |
        | type User implements Node @resolver {
        |   id: ID!
        |   name: String
        |   email: String
        | }
        |
        | input UserInput {
        |   name: String!
        |   email: String!
        | }
        |
        | extend type Query {
        |   user(id: String!): User @resolver
        | }
        |
        | extend type Mutation {
        |   createUser(input: UserInput!): User @resolver
        |   updateUser(id: String!, input: UserInput!): User @resolver
        | }
        |
        | #END_SCHEMA
        """.trimMargin()

    companion object {
        // In practice, our tests don't execute in parallel, but we make them thread-safe as a reminder
        // that mutations (and resolvers in general) need to be written in a thread-safe manner.
        // Structure: Map<InternalId, Pair<Name, Email>> - storing user data by internal ID
        private val users = ConcurrentHashMap<String, Pair<String, String>>() // id -> (name, email)
        private val nextId = AtomicInteger(1)
    }

    @BeforeEach
    fun cleanUp() {
        users.clear()
        nextId.set(1)
    }

    /**
     * Node Resolver for User objects. Handles creating User objects by GlobalID.
     *
     * The Node Resolver pattern is Viaduct's way of implementing the Relay Global Object
     * Identification specification. It allows fetching any object by its global ID,
     * which encodes both the type information and the internal ID.
     */
    class UserNodeResolver : NodeResolvers.User() {
        override suspend fun resolve(ctx: Context): User {
            // Extract the internal ID from the global ID - this is the actual key we use in our storage
            val internalId = ctx.id.internalID
            val (name, email) = users[internalId]
                ?: throw IllegalArgumentException("User not found: $internalId")

            return User.Builder(ctx)
                .id(ctx.id) // Use the original global ID passed in
                .name(name)
                .email(email)
                .build()
        }
    }

    /**
     * Query resolver to fetch a user by ID using the Node Resolver system.
     *
     * This demonstrates the proper way to query objects in Viaduct:
     * 1. Convert the string ID to a global ID for the User type
     * 2. Use the Node Resolver system to fetch the object
     * 3. Handle cases where the object doesn't exist gracefully
     */
    @Resolver
    class userResolver : QueryResolvers.User() {
        override suspend fun resolve(ctx: Context): User? {
            return try {
                // nodeFor() uses the registered Node Resolver to fetch the object
                // globalIDFor() creates a properly typed global ID from the string argument
                ctx.nodeFor(ctx.globalIDFor(User.Reflection, ctx.arguments.id))
            } catch (e: IllegalArgumentException) {
                null // Return null if user doesn't exist - GraphQL will handle this gracefully
            }
        }
    }

    /**
     * Mutation resolver to create a new user. Uses atomic increment for thread-safe ID generation.
     *
     * This shows the standard pattern for create mutations:
     * 1. Generate a unique internal ID
     * 2. Store the data using that ID
     * 3. Return a User object with a proper global ID
     */
    @Resolver
    class CreateUserResolver : MutationResolvers.CreateUser() {
        override suspend fun resolve(ctx: Context): User {
            val input = ctx.arguments.input
            // Generate thread-safe unique ID with descriptive prefix
            val newId = "user-${nextId.getAndIncrement()}"

            // Store in our thread-safe in-memory database
            users[newId] = Pair(input.name, input.email)

            // Build and return the User object with proper global ID
            // The global ID will encode both the type (User) and internal ID (newId)
            return User.Builder(ctx)
                .id(ctx.globalIDFor(User.Reflection, newId))
                .name(input.name)
                .email(input.email)
                .build()
        }
    }

    /**
     * Mutation resolver to update an existing user using Node Resolver system.
     *
     * This demonstrates atomic update operations and proper error handling:
     * - computeIfPresent() ensures the update is atomic
     * - Returns null if the user doesn't exist (GraphQL handles this gracefully)
     * - Uses the same ID that was passed in to maintain consistency
     */
    @Resolver
    class UpdateUserResolver : MutationResolvers.UpdateUser() {
        override suspend fun resolve(ctx: Context): User? {
            val input = ctx.arguments.input
            val id = ctx.arguments.id

            // Check if user exists and update atomically using computeIfPresent
            // This prevents race conditions where a user might be deleted between check and update
            val updated = users.computeIfPresent(id) { _, _ ->
                Pair(input.name, input.email)
            }

            return if (updated != null) {
                User.Builder(ctx)
                    .id(ctx.globalIDFor(User.Reflection, id))
                    .name(input.name)
                    .email(input.email)
                    .build()
            } else {
                null // User not found - mutation returns null
            }
        }
    }

    @Test
    fun `creates a new user`() {
        val result = execute(
            query = """
                mutation {
                    createUser(input: {
                        name: "John Doe"
                        email: "john@example.com"
                    }) {
                        id
                        name
                        email
                    }
                }
            """.trimIndent()
        )

        // Extract the global ID from the mutation result to use in subsequent queries
        // This demonstrates the proper way to handle IDs returned from mutations
        val createdUserId = result.getData<Map<String, Any>>()
            ?.get("createUser")?.let { it as Map<*, *> }
            ?.get("id") as String

        result.assertEquals {
            "data" to {
                "createUser" to {
                    "id" to createdUserId
                    "name" to "John Doe"
                    "email" to "john@example.com"
                }
            }
        }

        // Convert the global ID back to internal ID for querying
        // This shows the full round-trip: create -> get global ID -> extract internal ID -> query
        val globalId = getInternalId<User>(createdUserId)
        execute(
            query = """
                query {
                    user(id: "$globalId") {
                        name
                        email
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "user" to {
                    "name" to "John Doe"
                    "email" to "john@example.com"
                }
            }
        }
    }

    @Test
    fun `updates existing user`() {
        val createResult = execute(
            query = """
                mutation {
                    createUser(input: {
                        name: "Jane Doe"
                        email: "jane@example.com"
                    }) {
                        id
                    }
                }
            """.trimIndent()
        )

        val createdUserId = createResult.getData<Map<String, Any>>()
            ?.get("createUser")?.let { it as Map<*, *> }
            ?.get("id") as String

        val globalId = getInternalId<User>(createdUserId)

        execute(
            query = """
                mutation {
                    updateUser(id: "$globalId", input: {
                        name: "Jane Smith"
                        email: "jane.smith@example.com"
                    }) {
                        id
                        name
                        email
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "updateUser" to {
                    "id" to createdUserId
                    "name" to "Jane Smith"
                    "email" to "jane.smith@example.com"
                }
            }
        }
    }

    @Test
    fun `creates and gets user in full flow`() {
        val createResult = execute(
            query = """
                mutation {
                    createUser(input: {
                        name: "Bob Wilson"
                        email: "bob@example.com"
                    }) {
                        id
                    }
                }
            """.trimIndent()
        )

        val createdUserId = createResult.getData<Map<String, Any>>()
            ?.get("createUser")?.let { it as Map<*, *> }
            ?.get("id") as String

        val globalId = getInternalId<User>(createdUserId)

        execute(
            query = """
                query {
                    user(id: "$globalId") {
                        id
                        name
                        email
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "user" to {
                    "id" to createdUserId
                    "name" to "Bob Wilson"
                    "email" to "bob@example.com"
                }
            }
        }
    }
}
