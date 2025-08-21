package viaduct.testapps.schemaregistration

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertEquals
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

/**
 * Register schema filtering by tenantPackagePrefix tenant2
 * Tenant2 only has SCOPE2 fields and not SCOPE1 fields
 */
@ExperimentalCoroutinesApi
class RegisterSchemaFromResources2Test : TestBase(
    setOf(ScopedSchemaInfo(schemaId = "SCHEMA_ID_1", scopeIds = setOf("SCOPE1")), ScopedSchemaInfo(schemaId = "SCHEMA_ID_2", scopeIds = setOf("SCOPE2"))),
    tenantPackageFinder = TestTenantPackageFinder(listOf(viaduct.testapps.schemaregistration.tenant2.Tenant2Module::class)),
) {
    @Test
    fun `Resolve SCOPE2 field succeeds with SCHEMA_ID_2`() {
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
    fun `Fails to resolve SCOPE1 field in scoped schema SCHEMA_ID_2 because it contains SCOPE2 fields only`() {
        execute(
            schemaId = "SCHEMA_ID_2",
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
                    "message" to "Validation error (FieldUndefined@[scope1Value]) : Field 'scope1Value' in type 'Query' is undefined"
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
    fun `Fails to get data with SCOPE1 field`() {
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
                "scope1Value" to null
            }
        }
    }
}
