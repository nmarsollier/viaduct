@file:Suppress("unused", "ClassName")

package viaduct.tenant.tutorials.simplescopes

import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.graphql.test.assertEquals
import viaduct.service.runtime.SchemaRegistryConfiguration
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase
import viaduct.tenant.tutorials.simplescopes.resolverbases.QueryResolvers

/**
 * Demonstrates Viaduct's Scopes feature for API security and organization.
 *
 * Scopes allow you to deploy different versions of your GraphQL API for different clients.
 * Fields and types can be tagged with specific scopes, and only APIs registered with those
 * scopes will include them. This enables secure separation between customer-facing APIs,
 * admin dashboards, and internal tools using the same schema definition.
 *
 * Each API deployment gets registered with a set of scopes, controlling what fields
 * are available to that specific client or application.
 */
class SimpleScopesFeatureAppTest : FeatureAppTestBase() {
    override var sdl =
        """
        | #START_SCHEMA
        | extend type Query @scope(to: ["USER"]) {
        |   myOrders(userId: String!): [String!]! @resolver
        | }
        |
        | extend type Query @scope(to: ["ADMIN"]) {
        |   allUserData: [String!]! @resolver
        | }
        | #END_SCHEMA
        """.trimMargin()

    /**
     * Handles user-specific order queries for customer-facing APIs. Only available in
     * deployments with USER scope. Viaduct generates QueryResolvers.MyOrders() and wires
     * this resolver to the myOrders field automatically when USER scope is active.
     */
    @Resolver
    class MyOrdersResolver : QueryResolvers.MyOrders() {
        override suspend fun resolve(ctx: Context): List<String> {
            // Get userId from query arguments - in reality would validate against auth context
            val userId = ctx.arguments.userId

            // Static data simulation - in reality this would be a database call
            val userOrders = when (userId) {
                "user-123" -> listOf("Order #1001", "Order #1002")
                "user-456" -> listOf("Order #2001")
                else -> emptyList()
            }

            return userOrders
        }
    }

    /**
     * Provides admin access to sensitive user data for internal dashboards. Only available
     * in deployments with ADMIN scope. Returns data that should never be exposed to
     * customer-facing applications.
     */
    @Resolver
    class AllUserDataResolver : QueryResolvers.AllUserData() {
        override suspend fun resolve(ctx: Context): List<String> {
            // In reality, would query all users from database with sensitive info
            return listOf("User: john@example.com", "User: jane@example.com")
        }
    }

    @Test
    fun `Customer app can access user orders but not admin data`() {
        // `withSchemaRegistryConfiguration` lets you pass in a schema registry config
        // to configure the [Viaduct] object being tested.  In this example we're creating a
        // scoped schema named "CUSTOMER_API".  See Viaduct user docs for more on
        // schema scoping.
        withSchemaRegistryConfiguration(
            SchemaRegistryConfiguration.fromSdl(
                sdl,
                scopes = setOf(SchemaRegistryConfiguration.ScopeConfig("CUSTOMER_API", setOf("USER")))
            )
        )

        // User can see their orders - myOrders field is available because USER scope is included
        execute(
            query = """
                query {
                    myOrders(userId: "user-123")
                }
            """.trimIndent(),
            schemaId = "CUSTOMER_API"
        ).assertEquals {
            "data" to {
                "myOrders" to listOf("Order #1001", "Order #1002")
            }
        }

        // Admin data is not accessible - allUserData field doesn't exist in customer API
        // This prevents customers from accidentally accessing sensitive admin data
        execute(
            query = """
                query {
                    allUserData
                }
            """.trimIndent(),
            schemaId = "CUSTOMER_API"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Validation error (FieldUndefined@[allUserData]) : Field 'allUserData' in type 'Query' is undefined"
                    "locations" to arrayOf(
                        {
                            "line" to 2
                            "column" to 5
                        }
                    )
                    "extensions" to {
                        "classification" to "ValidationError"
                    }
                }
            )
            "data" to null
        }
    }

    @Test
    fun `Admin dashboard can access admin data but not user-specific data`() {
        // Register admin dashboard schema with ADMIN scope only
        // This simulates deploying the API for internal admin tools and dashboards
        withSchemaRegistryConfiguration(
            SchemaRegistryConfiguration.fromSdl(
                sdl,
                scopes = setOf(
                    SchemaRegistryConfiguration.ScopeConfig("ADMIN_API", setOf("ADMIN"))
                )
            )
        )

        // Admin can see all user data - allUserData field is available because ADMIN scope is included
        execute(
            query = """
                query {
                    allUserData
                }
            """.trimIndent(),
            schemaId = "ADMIN_API"
        ).assertEquals {
            "data" to {
                "allUserData" to listOf("User: john@example.com", "User: jane@example.com")
            }
        }

        // User orders field is not accessible - myOrders field doesn't exist in admin API
        // This keeps admin APIs focused on admin tasks, not user-specific operations
        execute(
            query = """
                query {
                    myOrders(userId: "user-123")
                }
            """.trimIndent(),
            schemaId = "ADMIN_API"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Validation error (FieldUndefined@[myOrders]) : Field 'myOrders' in type 'Query' is undefined"
                    "locations" to arrayOf(
                        {
                            "line" to 2
                            "column" to 5
                        }
                    )
                    "extensions" to {
                        "classification" to "ValidationError"
                    }
                }
            )
            "data" to null
        }
    }

    @Test
    fun `Internal tools with both scopes can access everything`() {
        // Register internal tools schema with both USER and ADMIN scopes
        // This simulates deploying the API for support tools that need access to everything
        withSchemaRegistryConfiguration(
            SchemaRegistryConfiguration.fromSdl(
                sdl,
                scopes = setOf(
                    SchemaRegistryConfiguration.ScopeConfig("INTERNAL_API", setOf("USER", "ADMIN"))
                )
            )
        )

        // Internal tools can access both user and admin fields
        // This is useful for customer support, debugging, and internal operations
        execute(
            query = """
                query {
                    myOrders(userId: "user-456")
                    allUserData
                }
            """.trimIndent(),
            schemaId = "INTERNAL_API"
        ).assertEquals {
            "data" to {
                "myOrders" to listOf("Order #2001") // Different user's data
                "allUserData" to listOf("User: john@example.com", "User: jane@example.com")
            }
        }
    }

    @Test
    fun `Unknown API deployment fails`() {
        // Try to use an API that was never deployed/registered
        // This simulates what happens when someone tries to use a non-existent API endpoint
        execute(
            query = """
                query {
                    myOrders(userId: "user-123")
                }
            """.trimIndent(),
            schemaId = "UNKNOWN_API"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Schema not found for schemaId=UNKNOWN_API"
                    "locations" to emptyList<String>() // No location info for schema-level errors
                    "extensions" to {
                        "classification" to "DataFetchingException" // Different error type than validation
                    }
                }
            )
        }
    }
}
