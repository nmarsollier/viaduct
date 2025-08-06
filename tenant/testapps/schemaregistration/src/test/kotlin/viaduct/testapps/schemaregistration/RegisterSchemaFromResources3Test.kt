package viaduct.testapps.schemaregistration

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertEquals
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

/**
 * Registers the schemas in tenant 1
 *
 * This test uses the tenantPackagePrefix to only include the tenant1 resources
 * in combination with the fullSchemaRegex to only include the tenant1 resources in the full schema
 */
class RegisterSchemaFromResources3Test : TestBase(
    setOf(ScopedSchemaInfo(schemaId = "SCHEMA_ID_1", scopeIds = setOf("SCOPE1"))),
    tenantPackageFinder = TestTenantPackageFinder(listOf(viaduct.testapps.schemaregistration.tenant1.Tenant1Module::class)),
    fullSchemaRegex = ".*tenant1.*graphqls",
) {
    @Test
    fun `Resolve SCOPE1 field succeeds with SCHEMA_ID_1`() {
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
    fun `Fails to execute query with SCOPE2 field because SCHEMA_ID_2 is not registered`() {
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
