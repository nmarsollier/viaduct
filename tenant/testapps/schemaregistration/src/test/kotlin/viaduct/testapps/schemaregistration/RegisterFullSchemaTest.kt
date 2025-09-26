package viaduct.testapps.schemaregistration

import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import viaduct.engine.api.ViaductSchema
import viaduct.graphql.test.assertEquals
import viaduct.graphql.utils.DefaultSchemaProvider
import viaduct.service.runtime.SchemaRegistryConfiguration
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

/**
 * Tests to validate correct registration using withFullSchema and registerFullSchema
 *
 * This test registers a full schema with queries as FULL_SCHEMA manually
 */
@ExperimentalCoroutinesApi
class RegisterFullSchemaTest : TestBase(
    customSchemaRegistryConfiguration = run {
        val schema = mkSchema(
            """
                type TestScope1Object {
                  strValue: String!
                }

                extend type Query {
                  scope1Value: TestScope1Object @resolver
                }

                type TestScope2Object {
                  strValue: String!
                }

                extend type Query {
                  scope2Value: TestScope2Object @resolver
                }
            """.trimIndent()
        )
        SchemaRegistryConfiguration.fromSchema(schema, fullSchemaIds = listOf("FULL_SCHEMA"))
    },
    tenantPackageFinder = TestTenantPackageFinder(Tenants)
) {
    @Test
    fun `Resolve execute fields in FULL_SCHEMA schema`() {
        execute(
            schemaId = "FULL_SCHEMA",
            query = """
                query {
                    scope1Value {
                        strValue
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "scope1Value" to {
                    "strValue" to "scope 1 value"
                }
            }
        }
    }

    @Test
    fun `Fails to resolve query when schema is not registered`() {
        execute(
            schemaId = "SCHEMA_ID",
            query = """
                query {
                    scope1Value {
                        strValue
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Schema not found for schemaId=SCHEMA_ID"
                    "locations" to emptyList<String>()
                    "extensions" to {
                        "classification" to "DataFetchingException"
                    }
                }
            )
        }
    }
}

private fun mkSchema(sdl: String): ViaductSchema =
    ViaductSchema(
        SchemaGenerator().makeExecutableSchema(
            SchemaParser().parse(sdl).apply {
                DefaultSchemaProvider.addDefaults(this)
            },
            RuntimeWiring.MOCKED_WIRING
        )
    )
