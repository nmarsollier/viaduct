package viaduct.testapps.scopes

import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertEquals
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

/**
 * Scoped tests for the [QueryResolvers] class.
 */
class Scopes2Test : TestBase(
    setOf(ScopedSchemaInfo("SCHEMA_ID_1", setOf("SCOPE1")), ScopedSchemaInfo("SCHEMA_ID_2", setOf("SCOPE2"))),
    tenantPackageFinder = TestTenantPackageFinder(Tenants),
) {
    @Test
    fun `Resolve SCOPE1 fields against SCHEMA_ID_1 schema succeeds`() {
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
    fun `Resolve SCOPE2 fields against SCHEMA_ID_2 schema succeeds`() {
        execute(
            schemaId = "SCHEMA_ID_2",
            query = """
                query {
                    scope2Value {
                        strValue
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "scope2Value" to {
                    "strValue" to "scope 2 value"
                }
            }
        }
    }
}
