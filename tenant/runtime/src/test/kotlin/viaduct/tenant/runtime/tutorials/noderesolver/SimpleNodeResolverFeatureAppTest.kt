@file:Suppress("unused", "ClassName")

package viaduct.tenant.runtime.tutorials.noderesolver

import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.graphql.test.assertEquals
import viaduct.graphql.test.hasError
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase
import viaduct.tenant.runtime.tutorials.noderesolver.resolverbases.QueryResolvers

/**
 * Demonstrates Viaduct's Node Resolver system for object resolution by GlobalID.
 *
 * Node Resolvers are the foundation for fetching objects that implement the Node interface.
 * When you call ctx.nodeFor() with a GlobalID, Viaduct automatically routes to the
 * appropriate Node Resolver. This creates a clean separation between object creation
 * logic and field resolution logic.
 */
class SimpleNodeResolverFeatureAppTest : FeatureAppTestBase() {
    override var sdl =
        """
        | #START_SCHEMA
        | type Foo implements Node @resolver {
        |   id: ID!
        |   bar: String!
        | }
        |
        | extend type Query {
        |   foo(id: String!): Foo! @resolver
        | }
        | #END_SCHEMA
        """.trimMargin()

    /**
     * Node Resolver for Foo objects. Viaduct generates Nodes.Foo() base class
     * from the schema and calls this resolver whenever ctx.nodeFor() is used
     * with a Foo GlobalID. This is where you define how to create/fetch Foo objects.
     */
    class FooNodeResolver : NodeResolvers.Foo() {
        override suspend fun resolve(ctx: Context): Foo {
            // Static data simulation - in reality this would be a database call
            val message = when (val internalID = ctx.id.internalID) {
                "foo-123" -> "Hello from the other Node!"
                "foo-456" -> "Another Foo object!"
                else -> throw IllegalArgumentException("Foo not found: $internalID")
            }

            return Foo.Builder(ctx)
                .id(ctx.id) // Use the GlobalID passed in
                .bar(message)
                .build()
        }
    }

    /**
     * Query Field Resolver that uses the Node Resolver system. Viaduct generates
     * QueryResolvers.foo() and wires this to the foo field.
     * Takes an ID argument and uses ctx.nodeFor() to let the framework route
     * to the appropriate Node Resolver automatically.
     */
    @Resolver
    class fooResolver : QueryResolvers.Foo() {
        override suspend fun resolve(ctx: Context): Foo {
            // Get ID from query arguments and let framework route to FooNodeResolver automatically
            return ctx.nodeFor(ctx.globalIDFor(Foo.Reflection, ctx.arguments.id))
        }
    }

    @Test
    fun `Query returns a node through node resolver`() {
        execute(
            query = """
                query TestQuery {
                   foo(id: "foo-123") {
                       id
                       bar
                   }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "foo" to {
                    // GlobalID is encoded string containing type + internal ID
                    "id" to createGlobalIdString(Foo.Reflection, "foo-123")
                    "bar" to "Hello from the other Node!"
                }
            }
        }
    }

    @Test
    fun `Query returns different foo data`() {
        execute(
            query = """
                query TestQuery {
                   foo(id: "foo-456") {
                       id
                       bar
                   }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "foo" to {
                    "id" to createGlobalIdString(Foo.Reflection, "foo-456")
                    "bar" to "Another Foo object!"
                }
            }
        }
    }

    @Test
    fun `Query throws error for unknown foo`() {
        // This would result in an error due to our FooNodeResolver validation
        execute(
            query = """
                query TestQuery {
                   foo(id: "unknown-foo") {
                       bar
                   }
                }
            """.trimIndent()
        ).hasError("Foo not found: unknown-foo")
    }
}
