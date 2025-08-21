package viaduct.testapps.schemaregistration

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertEquals
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

/**
 * Register the schema filtering by tenantPackagePrefix tenant1
 * Tenant1 only has SCOPE1 fields and not SCOPE2 fields
 */
@ExperimentalCoroutinesApi
class RegisterSchemaFromResources1Test : TestBase(
    setOf(ScopedSchemaInfo(schemaId = "SCHEMA_ID_1", scopeIds = setOf("SCOPE1")), ScopedSchemaInfo(schemaId = "SCHEMA_ID_2", scopeIds = setOf("SCOPE2"))),
    tenantPackageFinder = TestTenantPackageFinder(listOf(viaduct.testapps.schemaregistration.tenant1.Tenant1Module::class)),
) {
    @Test
    fun `Resolve query with SCOPE1 field succeeds with SCHEMA_ID_1`() {
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
    fun `Fails to get data for SCOPE2 fields because cannot find SCOPE2 field resolver in tenant1 package prefix`() {
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
                "scope2Value" to null
            }
        }
    }
}
