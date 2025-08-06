@file:Suppress("unused", "ClassName")

package viaduct.tenant.runtime.execution.variables.trivial

import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.api.Variable
import viaduct.api.Variables
import viaduct.api.VariablesProvider
import viaduct.api.types.Arguments
import viaduct.graphql.test.assertEquals
import viaduct.tenant.runtime.execution.variables.trivial.resolverbases.QueryResolvers
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase

class VariablesProviderFeatureAppTest : FeatureAppTestBase() {
    override var sdl =
        """
        | #START_SCHEMA
        | directive @resolver on FIELD_DEFINITION
        | type Query {
        |   fromArgumentField(arg: Int!): Int @resolver
        |   intermediary(arg: Int!): Int @resolver
        |   fromVariablesProvider: Int @resolver
        | }
        |
        | #END_SCHEMA
        """.trimMargin()

    @Resolver(
        """
        fragment _ on Query {
           intermediary(arg: ${'$'}myVar)
        }
        """,
        variables = [Variable("myVar", fromArgument = "arg")]
    )
    class Query_FromArgumentFieldResolver : QueryResolvers.FromArgumentField() {
        override suspend fun resolve(ctx: Context): Int = ctx.objectValue.get("intermediary", Int::class)
    }

    @Resolver
    class Query_IntermediaryResolver : QueryResolvers.Intermediary() {
        override suspend fun resolve(ctx: Context): Int = ctx.arguments.arg
    }

    @Resolver(
        """
        fragment _ on Query {
            intermediary(arg: ${'$'}x)
        }
        """
    )
    class Query_FromVariablesProviderResolver : QueryResolvers.FromVariablesProvider() {
        override suspend fun resolve(ctx: Context): Int = ctx.objectValue.get("intermediary", Int::class)

        @Variables("x: Int!")
        class TestVariablesProvider : VariablesProvider<Arguments> {
            override suspend fun provide(args: Arguments): Map<String, Any?> = mapOf("x" to 123)
        }
    }

    @Test
    fun `variables via variables parameter`() {
        execute(
            query = """
                query {
                    fromArgumentField(arg: 7)
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "fromArgumentField" to 7
            }
        }
    }

    @Test
    fun `variables via VariablesProvider`() {
        execute("{ fromVariablesProvider }").assertEquals {
            "data" to { "fromVariablesProvider" to 123 }
        }
    }
}
