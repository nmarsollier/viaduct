@file:Suppress("unused", "ClassName")

package viaduct.tenant.runtime.execution.node

import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.graphql.test.assertEquals
import viaduct.tenant.runtime.execution.node.resolverbases.QueryResolvers
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase

class NodeResolverFeatureAppTest : FeatureAppTestBase() {
    override var sdl =
        """
        | #START_SCHEMA
        | directive @resolver on FIELD_DEFINITION | OBJECT
        |
        | interface Node {
        |     id: ID!
        | }
        | type TestGlobalId implements Node @resolver {
        |     id: ID!
        |     value: String!
        | }
        |
        | type Query {
        |   getGlobalID(id: String!): TestGlobalId! @resolver
        |   nodeReference(id: String!): TestGlobalId! @resolver
        | }
        | #END_SCHEMA
        """.trimMargin()

    // Tenant provided resolvers

    @Resolver
    class GetGlobalIDResolver : QueryResolvers.GetGlobalID() {
        override suspend fun resolve(ctx: Context): TestGlobalId {
            return TestGlobalId.Builder(ctx)
                .id(ctx.globalIDFor(TestGlobalId.Reflection, ctx.arguments.id))
                .value(ctx.arguments.id)
                .build()
        }
    }

    @Resolver
    class NodeReferenceResolver : QueryResolvers.NodeReference() {
        override suspend fun resolve(ctx: Context): TestGlobalId {
            return ctx.nodeFor(ctx.globalIDFor(TestGlobalId.Reflection, ctx.arguments.id))
        }
    }

    class TestGlobalIdResolver : Nodes.TestGlobalId() {
        override suspend fun resolve(ctx: Context): TestGlobalId {
            return TestGlobalId.Builder(ctx).id(ctx.id).build()
        }
    }

    // Test Cases Start here
    @Test
    fun `Resolver returns the new GlobalID structured type (not the old string alias)`() {
        val generatedId = createGlobalIdString(TestGlobalId.Reflection, "tenant1")

        execute(
            query = """
                    query TestQuery {
                        getGlobalID(id: "tenant1") {
                            id
                            value
                        }
                    }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "getGlobalID" to {
                    "value" to "tenant1"
                    "id" to generatedId
                }
            }
        }
    }

    @Test
    fun `Resolver returns a node reference`() {
        val generatedId = createGlobalIdString(TestGlobalId.Reflection, "tenant1")

        execute(
            query = """
                    query TestQuery {
                        nodeReference(id: "tenant1") {
                            id
                        }
                    }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "nodeReference" to {
                    "id" to generatedId
                }
            }
        }
    }
}
