package viaduct.testapps.resolver

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertEquals
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

/**
 * Tests cross tenant @Resolver.
 */
class CrossTenantResolverTests : TestBase(
    setOf(ScopedSchemaInfo(DEFAULT_SCHEMA_ID, setOf(DEFAULT_PUBLIC_SCOPE_ID))),
    tenantPackageFinder = TestTenantPackageFinder(Tenants),
) {
    // Also tests :
    // The nested @Resolver field is set in the top-level @Resolver
    // The nested @Resolver field is not set in the top-level @Resolver
    @Test
    fun `Resolver calling another Resolver field in its fragment (nested Resolver succeeds)`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                query TestQuery {
                    nestedField {
                        value
                        nestedValue
                        nestedObject {
                            value
                        }
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "nestedField" to {
                    "value" to "string value"
                    "nestedValue" to "tenant1 value"
                    "nestedObject" to {
                        "value" to "tenant2 value"
                    }
                }
            }
        }
    }

    @Test
    fun `Resolver calling another Resolver field in its fragment (nested Resolver throws an exception)`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                query TestQuery {
                    nestedFieldException {
                        value
                        nestedValue
                        nestedObject {
                            value
                        }
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Exception while fetching data (/nestedFieldException) : viaduct.api.ViaductTenantUsageException: Attempted to access field Query.throwsException " +
                        "but it was not set: please add it to the @Resolver fragment"
                    "locations" to arrayOf(
                        {
                            "line" to 2
                            "column" to 5
                        }
                    )
                    "path" to listOf("nestedFieldException")
                    "extensions" to {
                        "classification" to "DataFetchingException"
                    }
                }
            )
            "data" to {
                "nestedFieldException" to null
            }
        }
    }

    @Test
    fun `Resolver calling another Resolver field in its fragment (fragments undefined)`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                query TestQuery {
                    nestedFieldFragmentError {
                        value
                        nestedValue
                        nestedObject {
                            value
                        }
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Exception while fetching data (/nestedFieldFragmentError) : viaduct.api.ViaductTenantUsageException: " +
                        "Attempted to access field Query.throwsException but it was not set: please add it to the @Resolver fragment"
                    "locations" to arrayOf(
                        {
                            "line" to 2
                            "column" to 5
                        }
                    )
                    "path" to listOf("nestedFieldFragmentError")
                    "extensions" to {
                        "classification" to "DataFetchingException"
                    }
                }
            )
            "data" to {
                "nestedFieldFragmentError" to null
            }
        }
    }

    @Test
    fun `Resolver throws an exception`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { throwsException }"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Exception while fetching data (/throwsException) : java.lang.NullPointerException: This is a test exception"
                    "locations" to arrayOf(
                        {
                            "line" to 1
                            "column" to 19
                        }
                    )
                    "path" to listOf("throwsException")
                    "extensions" to {
                        "classification" to "DataFetchingException"
                    }
                }
            )
            "data" to {
                "throwsException" to null
            }
        }
    }

    @Test
    fun `Resolver returns an object type where there object type returned itself has a Resolver field`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                query TestQuery {
                    resolvedNestedObject {
                        value
                        resolvedObject {
                            strValue
                        }
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "resolvedNestedObject" to {
                    "value" to "string value"
                    "resolvedObject" to {
                        "strValue" to "tenant1 value"
                    }
                }
            }
        }
    }

    @Test
    fun `Resolver tries to read a field that is not in its required selections`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                    query TestQuery {
                        nestedFieldError {
                            nestedValue {
                                strValue
                                enumValue
                            }
                        }
                    }
            """.trimIndent()
        ).let { resp ->
            assertEquals(mapOf("nestedFieldError" to null), resp.getData())

            assertEquals(1, resp.errors.size)
            val err = resp.errors.first().toSpecification()
            assertEquals(listOf("nestedFieldError"), err["path"])
            assertEquals(mapOf("classification" to "DataFetchingException"), err["extensions"])
        }
    }
}
