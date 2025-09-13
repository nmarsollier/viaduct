package viaduct.testapps.schemaregistration

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertEquals
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

/**
 * Tests to validate correct registration using withFullSchemaFromSdl and registerSchemaFromSdl
 *
 * This test registers a full schema with queries as PUBLIC manually
 */
@ExperimentalCoroutinesApi
class RegisterSchemaFromSdlTest : TestBase(
    customSchemaRegistration = {
        val sdl = """
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
        """
        it.withFullSchemaFromSdl(sdl)
        it.registerSchemaFromSdl("PUBLIC", sdl)
    },
    tenantPackageFinder = TestTenantPackageFinder(Tenants),
) {
    @Test
    fun `Resolve executes query against PUBLIC schema`() {
        execute(
            schemaId = "PUBLIC",
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
    fun `Fails to execute query because SCHEMA_ID_2 is not registered`() {
        execute(
            schemaId = "SCHEMA_ID_2",
            query = """
                query {
                    scope1Value {
                        strValue
                    }
                }
            """.trimIndent()
        )
            .assertEquals {
                "errors" to arrayOf(
                    {
                        "message" to "Schema not found for schemaId=SCHEMA_ID_2"
                        "locations" to emptyList<String>()
                        "extensions" to {
                            "classification" to "DataFetchingException"
                        }
                    }
                )
            }
    }
}
