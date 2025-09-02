@file:Suppress("unused", "ClassName")

package viaduct.tenant.runtime.tutorials.fieldresolver

import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.graphql.test.assertEquals
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase
import viaduct.tenant.runtime.tutorials.fieldresolver.resolverbases.QueryResolvers

/**
 * Demonstrates the simplest Viaduct resolver - a Field Resolver.
 *
 * Field Resolvers handle individual GraphQL fields by computing their values.
 * The @resolver directive generates base classes from the schema, and you implement
 * the resolve() function to return the field's value. Viaduct handles all the
 * wiring between GraphQL queries and your business logic.
 */
class SimpleFieldResolverFeatureAppTest : FeatureAppTestBase() {
    override var sdl =
        """
        | #START_SCHEMA
        | directive @resolver on FIELD_DEFINITION | OBJECT
        |
        | type Query {
        |   foo: String! @resolver
        | }
        |
        | #END_SCHEMA
        """.trimMargin()

    /**
     * The simplest resolver - computes a value for a single field.
     * Viaduct generates QueryResolvers.Foo() base class from the schema
     * and automatically calls this resolver when "foo" is requested in any GraphQL query.
     */
    @Resolver // Registers this resolver with the framework
    class FooResolver : QueryResolvers.Foo() {
        override suspend fun resolve(ctx: Context): String {
            // Return computed value - could be from database, API, or any custom logic
            return "bar"
        }
    }

    @Test
    fun `Query returns a field resolver`() {
        execute(
            query = """
                query TestQuery {
                   foo
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "foo" to "bar" // Value computed by FooResolver
            }
        }
    }
}
