package viaduct.testapps.scopes

import kotlin.arrayOf
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertEquals
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

/**
 * Scoped tests for the [QueryResolvers] class.
 */
class ScopesTest : TestBase(
    setOf(ScopedSchemaInfo("SCHEMA_ID_1", setOf("SCOPE1"))),
    tenantPackageFinder = TestTenantPackageFinder(viaduct.testapps.scopes.Tenants),
) {
    @Test
    fun `Resolve query with SCOPE1 fields against SCHEMA_ID_1 schema succeeds`() {
        execute(
            schemaId = "SCHEMA_ID_1",
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
    fun `Resolve fails to run query with SCOPE2 fields against SCHEMA_ID_1 schema`() {
        execute(
            schemaId = "SCHEMA_ID_1",
            query = """
                query {
                    scope2Value {
                        strValue
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Validation error (FieldUndefined@[scope2Value]) : Field 'scope2Value' in type 'Query' is undefined"
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
    fun `Resolve query with SCOPE2 fields fails as SCHEMA_ID_2 is not registered`() {
        execute(
            schemaId = "SCHEMA_ID_2",
            query = """
                query {
                    scope2Value {
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
