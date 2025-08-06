package viaduct.testapps.resolver

import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertEquals
import viaduct.graphql.test.assertJson
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

/**
 * Test @Resolver for @backingData queries.
 */
class BackingDataResolverTests : TestBase(
    setOf(ScopedSchemaInfo(DEFAULT_SCHEMA_ID, setOf(DEFAULT_PUBLIC_SCOPE_ID))),
    tenantPackageFinder = TestTenantPackageFinder(Tenants),
) {
    @Test
    fun `Resolver returns a backing data type`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                query TestQuery {
                    backingData {
                        strValue
                    }
                }
            """.trimIndent()
        ).assertJson("""{"data":{"backingData":{"strValue":"backing data value"}}}""")
    }

    @Test
    fun `Resolver includes a backing data fields in its required selections`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                    query TestQuery {
                        backingData {
                            backingDataValue {
                                strValue,
                                intValue
                            }
                        }
                    }
            """.trimIndent()
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Validation error (SubselectionNotAllowed@[backingData/backingDataValue]) : Subselection not allowed on leaf type " +
                        "'BackingData' of field 'backingDataValue'"
                    "locations" to arrayOf(
                        {
                            "line" to 3
                            "column" to 9
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
    fun `Resolver includes a backing data type in its required selections`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                    query TestQuery {
                        backingData {
                            backingDataValue
                        }
                    }
            """.trimIndent()
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "serialize should not be called for BackingData scalar type. This is a no-op."
                    "locations" to arrayOf(
                        {
                            "line" to 3
                            "column" to 9
                        }
                    )
                    "path" to listOf("backingData", "backingDataValue")
                    "extensions" to {
                        "classification" to "VIADUCT_INTERNAL_ENGINE_EXCEPTION"
                    }
                }
            )
            "data" to {
                "backingData" to {
                    "backingDataValue" to null
                }
            }
        }
    }
}
