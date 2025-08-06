package viaduct.testapps.schemaregistration

import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertEquals
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

/**
 * This rest register the SCOPE1, SCOPE2, as normal test should work
 */
class RegisterSchemaFromResourcesTest : TestBase(
    setOf(ScopedSchemaInfo(schemaId = "SCHEMA_ID_1", scopeIds = setOf("SCOPE1")), ScopedSchemaInfo(schemaId = "SCHEMA_ID_2", scopeIds = setOf("SCOPE2"))),
    tenantPackageFinder = TestTenantPackageFinder(Tenants),
) {
    @Test
    fun `Resolve SCOPE2 field with SCHEMA_ID_2 succeeds`() {
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

    @Test
    fun `Resolve SCOPE1 field with SCHEMA_ID_1 succeeds`() {
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
}
