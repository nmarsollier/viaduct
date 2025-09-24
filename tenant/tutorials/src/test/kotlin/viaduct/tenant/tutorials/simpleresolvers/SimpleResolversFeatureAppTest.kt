@file:Suppress("unused", "ClassName")

package viaduct.tenant.tutorials.simpleresolvers

import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.graphql.test.assertEquals
import viaduct.graphql.test.hasError
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase
import viaduct.tenant.tutorials.simpleresolvers.resolverbases.QueryResolvers
import viaduct.tenant.tutorials.simpleresolvers.resolverbases.UserResolvers

/**
 * Demonstrates Node Resolvers and Field Resolvers working together.
 *
 * This combines both patterns: Node Resolvers handle object creation by GlobalID,
 * while Field Resolvers compute individual field values. The objectValueFragment
 * feature ensures that computed fields automatically get the parent data they need,
 * even when the query doesn't explicitly request those fields.
 */
class SimpleResolversFeatureAppTest : FeatureAppTestBase() {
    override var sdl =
        """
        | #START_SCHEMA
        |
        | type User implements Node @resolver {
        |   id: ID!
        |   firstname: String!
        |   lastname: String!
        |   fullName: String! @resolver
        | }
        |
        | extend type Query {
        |   user(id: String!): User! @resolver
        | }
        | #END_SCHEMA
        """.trimMargin()

    /**
     * Node Resolver for User objects. Viaduct generates Nodes.User() base class
     * and calls this resolver when ctx.nodeFor() is used with a User GlobalID.
     * Handles creating/fetching User objects with their basic data.
     */
    class UserNodeResolver : NodeResolvers.User() {
        override suspend fun resolve(ctx: Context): User {
            // Extract internal ID from GlobalID - this would typically be used for DB lookup
            // Static data simulation - in reality this would be a database call
            val userData = when (val internalID = ctx.id.internalID) {
                "john-doe" -> Pair("John", "Doe")
                "jane-doe" -> Pair("Jane", "Doe")
                else -> throw IllegalArgumentException("User not found: $internalID")
            }

            return User.Builder(ctx)
                .id(ctx.id) // Use the GlobalID passed in
                .firstname(userData.first)
                .lastname(userData.second)
                .build()
        }
    }

    /**
     * Query Field Resolver that uses the Node Resolver system. Viaduct generates
     * QueryResolvers.user() and wires this to handle the getUser field.
     * Takes an ID argument and resolves it through the Node Resolver system.
     */
    @Resolver
    class GetUserResolver : QueryResolvers.User() {
        override suspend fun resolve(ctx: Context): User {
            // Get ID from query arguments and create GlobalID for Node Resolver lookup
            return ctx.nodeFor(ctx.globalIDFor(User.Reflection, ctx.arguments.id))
        }
    }

    /**
     * Field Resolver that computes the fullName field on User objects.
     * Viaduct generates UserResolvers.FullName() base class from the schema.
     *
     * objectValueFragment: Tells Viaduct which parent fields this resolver needs.
     * Even if query only asks for "fullName", Viaduct auto-fetches "firstname" and "lastname"
     * from the parent User object so this resolver can access them.
     */
    @Resolver(
        objectValueFragment = "fragment _ on User { firstname lastname }"
    )
    class User_FullNameResolver : UserResolvers.FullName() {
        override suspend fun resolve(ctx: Context): String {
            // Access parent User data via ctx.objectValue (guaranteed by objectValueFragment)
            val firstname = ctx.objectValue.getFirstname()
            val lastname = ctx.objectValue.getLastname()
            return "$firstname $lastname"
        }
    }

    // Test Cases Start here
    @Test
    fun `Query returns user with computed fullName`() {
        execute(
            query = """
                    query TestQuery {
                        user(id: "john-doe") {
                            firstname
                            lastname
                            fullName
                        }
                    }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "user" to {
                    "firstname" to "John"
                    "lastname" to "Doe"
                    "fullName" to "John Doe" // Computed by User_FullNameResolver
                }
            }
        }
    }

    @Test
    fun `Query returns different user data`() {
        execute(
            query = """
                    query TestQuery {
                        user(id: "jane-doe") {
                            firstname
                            lastname
                            fullName
                        }
                    }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "user" to {
                    "firstname" to "Jane"
                    "lastname" to "Doe"
                    "fullName" to "Jane Doe"
                }
            }
        }
    }

    @Test
    fun `Query throws error for unknown user`() {
        // This would result in an error due to our UserNodeResolver validation
        execute(
            query = """
                    query TestQuery {
                        user(id: "unknown-user") {
                            firstname
                        }
                    }
            """.trimIndent()
        ).hasError("User not found: unknown-user")
    }
}
