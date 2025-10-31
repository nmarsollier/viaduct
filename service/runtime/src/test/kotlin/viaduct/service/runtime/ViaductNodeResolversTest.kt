@file:Suppress("ForbiddenImport")

package viaduct.service.runtime

import com.google.common.net.UrlEscapers
import java.util.Base64
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import viaduct.engine.api.FieldResolverExecutor
import viaduct.engine.api.mocks.MockSchema
import viaduct.engine.api.mocks.MockTenantModuleBootstrapper
import viaduct.engine.api.mocks.mkEngineObjectData
import viaduct.engine.api.mocks.runFeatureTest
import viaduct.graphql.test.assertJson
import viaduct.service.runtime.noderesolvers.ViaductQueryNodeResolverModuleBootstrapper

@OptIn(ExperimentalCoroutinesApi::class)
class ViaductNodeResolversTest {
    companion object {
        private const val SCHEMA = """
              extend type Query {
                  user(id: ID!): User
              }

              type User implements Node {
                  id: ID!
                  name: String!
              }

              interface InterfaceType {
                  irrelevant: String!
              }

              type NonNode {
                  irrelevant: String!
              }
          """
        private val bootstrapper = MockTenantModuleBootstrapper(SCHEMA) {
            type("User") {
                nodeUnbatchedExecutor { id, _, _ ->
                    mkEngineObjectData(
                        schema.schema.getObjectType("User"),
                        mapOf("id" to id, "name" to "User-$id")
                    )
                }
            }
        }

        private const val INTERNAL_ID_1 = "123"
        private val GLOBAL_ID_1 = getGlobalId("User", INTERNAL_ID_1)
        private const val INTERNAL_ID_2 = "456"
        private val GLOBAL_ID_2 = getGlobalId("User", INTERNAL_ID_2)

        /**
         * Logic for endcoding the GlobalID string format.
         * This is copied from GlobalIDCodecImpl and should be refactored as part of:
         * https://app.asana.com/1/150975571430/project/1209554365854885/task/1211213956653747
         */
        private fun getGlobalId(
            typeName: String,
            internalID: String
        ): String =
            Base64.getEncoder().encodeToString(
                arrayListOf(
                    typeName,
                    UrlEscapers.urlFormParameterEscaper().escape(internalID)
                ).joinToString(":").toByteArray()
            )
    }

    @Test
    fun `node query with globalId resolves automatically`() {
        bootstrapper
            .runFeatureTest {
                runQuery(
                    """
                  query TestNodeQuery {
                      node(id: "$GLOBAL_ID_1") {
                          ... on User {
                              id
                              name
                          }
                      }
                  }
              """
                ).assertJson(
                    """
                    {data: {node: {id: "$GLOBAL_ID_1", name: "User-$GLOBAL_ID_1"}}}"
                """
                )
            }
    }

    @Test
    fun `nodes query with mulitiple globalIds resolves automatically`() {
        bootstrapper
            .runFeatureTest {
                runQuery(
                    """
                  query TestNodeQuery {
                      nodes(ids: ["$GLOBAL_ID_1", "$GLOBAL_ID_2"]) {
                          ... on User {
                              id
                              name
                          }
                      }
                  }
              """
                ).assertJson(
                    """
                    {data: { nodes: [
                            {id: "$GLOBAL_ID_1", name: "User-$GLOBAL_ID_1"},
                            {id: "$GLOBAL_ID_2", name: "User-$GLOBAL_ID_2"}
                        ] } }
                        """
                )
            }
    }

    @Test
    fun `node query with globalId returns null with framework provided resolver disabled`() {
        bootstrapper
            .runFeatureTest(withoutDefaultQueryNodeResolvers = true) {
                runQuery(
                    """
                  query TestNodeQuery {
                      node(id: "$INTERNAL_ID_1") {
                          ... on User {
                              id
                              name
                          }
                      }
                  }
              """
                ).assertJson("{data: {node: null}}")
            }
    }

    @Test
    fun `nodes query with multiple globalIds errors with framework provided resolver disabled`() {
        // Per the schema Query.nodes cannot be null. However, if a resolver is not present,
        // Graphql defaults to null even if the field type is an array which violates the schema and errors.
        bootstrapper
            .runFeatureTest(withoutDefaultQueryNodeResolvers = true) {
                val result = runQuery(
                    """
                  query TestNodeQuery {
                      nodes(ids: ["$GLOBAL_ID_1", "$GLOBAL_ID_2"]) {
                          ... on User {
                              id
                              name
                          }
                      }
                  }
              """
                )

                assertEquals(1, result.errors.size)
                val error = result.errors[0]
                assertTrue(error.message.contains("The non-nullable type is '[Node]' within parent type 'Query'"))
            }
    }

    @Test
    fun `errors with a syntactically invalid global id string`() {
        bootstrapper
            .runFeatureTest {
                val invalidGlobalId = getGlobalId("Invalid:Syntax", INTERNAL_ID_1)

                val result = runQuery(
                    """
                  query TestNodeQuery {
                      node(id: "$invalidGlobalId") {
                          ... on User {
                              id
                              name
                          }
                      }
                  }
              """
                )

                result.getData<Map<String, Any?>>().assertJson("{node: null}")
                assertEquals(1, result.errors.size)
                val error = result.errors[0]
                assertTrue(error.message.contains("Expected GlobalID \"$invalidGlobalId\" to be a Base64-encoded string with the decoded format '<type name>:<internal ID>'"))
            }
    }

    @Test
    fun `errors with a syntactically valid global id that does not exist`() {
        bootstrapper
            .runFeatureTest {
                val nonexistentGlobalId = getGlobalId("People", INTERNAL_ID_1)

                val result = runQuery(
                    """
                  query TestNodeQuery {
                      node(id: "$nonexistentGlobalId") {
                          ... on User {
                              id
                              name
                          }
                      }
                  }
              """
                )

                result.getData<Map<String, Any?>>().assertJson("{node: null}")
                assertEquals(1, result.errors.size)
                val error = result.errors[0]
                assertTrue(error.message.contains("Expected GlobalId \"$nonexistentGlobalId\" with type name 'People' to match a named object type in the schema"))
            }
    }

    @Test
    fun `errors with a global id that exists but is not an object type`() {
        bootstrapper
            .runFeatureTest {
                val interfaceTypeGlobalId = getGlobalId("InterfaceType", INTERNAL_ID_1)

                val result = runQuery(
                    """
                  query TestNodeQuery {
                      node(id: "$interfaceTypeGlobalId") {
                          ... on User {
                              id
                              name
                          }
                      }
                  }
              """
                )

                result.getData<Map<String, Any?>>().assertJson("{node: null}")
                assertEquals(1, result.errors.size)
                val error = result.errors[0]
                assertTrue(
                    error.message.contains(
                        "graphql.AssertException: You have asked for named object type 'InterfaceType' but it's not an object type but rather a 'graphql.schema.GraphQLInterfaceType'"
                    )
                )
            }
    }

    @Test
    fun `errors with a global id that is an object type but does not extend node interface `() {
        bootstrapper
            .runFeatureTest {
                val nonNodeTypeGlobalId = getGlobalId("NonNode", INTERNAL_ID_1)

                val result = runQuery(
                    """
                  query TestNodeQuery {
                      node(id: "$nonNodeTypeGlobalId") {
                          ... on User {
                              id
                              name
                          }
                      }
                  }
              """
                )

                result.getData<Map<String, Any?>>().assertJson("{node: null}")
                assertEquals(1, result.errors.size)
                val error = result.errors[0]
                assertTrue(
                    error.message.contains("Expected GlobalId \"$nonNodeTypeGlobalId\" with type name 'NonNode' to match a named object type that extends the Node interface")
                )
            }
    }

    @Test
    fun `undefined query node and nodes fields with query node resolvers enabled does not break standard viaduct bootstrapping`() {
        val schemaWithoutNodes = """
              extend type Query {
                user: User
              }

              type User {
                  name: String!
              }
          """
        MockTenantModuleBootstrapper(schemaWithoutNodes) {
            field("Query" to "user") {
                resolver {
                    fn { _, _, _, _, _ ->
                        mkEngineObjectData(
                            schema.schema.getObjectType("User"),
                            mapOf("name" to "test-name")
                        )
                    }
                }
            }
        }.runFeatureTest {
            runQuery(
                """
                  query _ {
                      user {
                          name
                      }
                  }
              """
            ).assertJson("{data: {user: {name: 'test-name'}}}")
        }
    }

    @Nested
    inner class DirectNodeResolverTests {
        // These states should not be possible.
        // Testing the resolver directly to bypass GraphQL type coercion and validation.
        @Test
        fun `errors with a non-string global id`() {
            val fieldResolver = ViaductQueryNodeResolverModuleBootstrapper.queryNodeResolver

            val mockSelector = FieldResolverExecutor.Selector(
                arguments = mapOf("id" to 123), // Non-string id
                objectValue = mkEngineObjectData(MockSchema.minimal.schema.queryType, emptyMap()),
                queryValue = mkEngineObjectData(MockSchema.minimal.schema.queryType, emptyMap()),
                selections = null
            )

            runBlocking {
                val mockContext = bootstrapper.contextMocks.engineExecutionContext
                val result = fieldResolver.batchResolve(listOf(mockSelector), mockContext)
                val error = result[mockSelector]?.exceptionOrNull()
                assertTrue(error!!.message!!.contains("Expected GlobalID \"123\" to be a string. This should never occur."))
            }
        }

        @Test
        fun `errors with a non-list ids argument`() {
            val fieldResolver = ViaductQueryNodeResolverModuleBootstrapper.queryNodesResolver

            val mockSelector = FieldResolverExecutor.Selector(
                arguments = mapOf("ids" to "123"), // Non-list ids
                objectValue = mkEngineObjectData(MockSchema.minimal.schema.queryType, emptyMap()),
                queryValue = mkEngineObjectData(MockSchema.minimal.schema.queryType, emptyMap()),
                selections = null
            )

            runBlocking {
                val mockContext = bootstrapper.contextMocks.engineExecutionContext
                val result = fieldResolver.batchResolve(listOf(mockSelector), mockContext)
                val error = result[mockSelector]?.exceptionOrNull()
                assertTrue(error!!.message!!.contains("Expected 'ids' argument to be a list. This should never occur."))
            }
        }
    }
}
