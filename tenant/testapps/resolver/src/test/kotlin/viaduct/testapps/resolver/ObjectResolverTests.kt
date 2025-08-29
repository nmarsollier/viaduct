package viaduct.testapps.resolver

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertEquals
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

/**
 * Test @Resolver for object queries.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObjectResolverTests : TestBase(
    setOf(ScopedSchemaInfo(DEFAULT_SCHEMA_ID, setOf(DEFAULT_PUBLIC_SCOPE_ID))),
    tenantPackageFinder = TestTenantPackageFinder(Tenants),
) {
    @Test
    fun `Resolver returns an object type`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                query TestQuery {
                    objectValue {
                        strValue
                        optStrValue
                        enumValue
                        optEnumValue
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "objectValue" to {
                    "strValue" to "tenant1 value"
                    "optStrValue" to "tenant1 optional value"
                    "enumValue" to "VALUE1"
                    "optEnumValue" to "VALUE2"
                }
            }
        }
    }

    @Test
    fun `Resolver returns a list of object type`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { listObjectValue { strValue,  enumValue } }"
        ).assertEquals {
            "data" to {
                "listObjectValue" to arrayOf(
                    {
                        "strValue" to "value 1"
                        "enumValue" to "VALUE1"
                    },
                    {
                        "strValue" to "value 2"
                        "enumValue" to "VALUE2"
                    }
                )
            }
        }
    }

    @Test
    fun `Resolver returns an interface type`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { interfaceValue { strValue } }"
        ).assertEquals {
            "data" to {
                "interfaceValue" to {
                    "strValue" to "tenant1 value"
                }
            }
        }
    }

    @Test
    fun `Resolver with arguments returns an object type`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                query TestQuery {
                    objectOptionalWithArgs(strValue : "test value", enumValue : VALUE1) {
                        strValue,
                        optStrValue,
                        enumValue,
                        optEnumValue
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "objectOptionalWithArgs" to {
                    "strValue" to "test value"
                    "optStrValue" to "test value"
                    "enumValue" to "VALUE1"
                    "optEnumValue" to "VALUE1"
                }
            }
        }
    }

    @Test
    fun `Resolver with arguments null returns an object type`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                query TestQuery {
                    objectOptionalWithArgs(strValue : null, enumValue: null) {
                        strValue,
                        optStrValue,
                        enumValue,
                        optEnumValue
                    }
                }
            """.trimIndent()
        )
            .assertEquals {
                "data" to {
                    "objectOptionalWithArgs" to {
                        "strValue" to "default"
                        "optStrValue" to null
                        "enumValue" to "VALUE1"
                        "optEnumValue" to null
                    }
                }
            }
    }

    @Test
    fun `Resolver returns an object type with a non-null field`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { objectValue { optStrValue } }"
        ).assertEquals {
            "data" to {
                "objectValue" to {
                    "optStrValue" to "tenant1 optional value"
                }
            }
        }
    }

    @Test
    fun `Resolver returns an object type with a optional field`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { objectValue { strValue } }"
        ).assertEquals {
            "data" to {
                "objectValue" to {
                    "strValue" to "tenant1 value"
                }
            }
        }
    }

    @Test
    fun `Resolver returns an object type with a missing non-null field`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { invalidNullValue { strValue, optStrValue } }"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "The field at path '/invalidNullValue/strValue' was declared as a non null type, but the code involved in retrieving data has wrongly " +
                        "returned a null value.  The graphql specification requires that the parent field be set to null, or if that is non nullable that it bubble up " +
                        "null to its parent and so on. The non-nullable type is 'String' within parent type 'TestObjectValue'"
                    "path" to listOf("invalidNullValue", "strValue")
                    "extensions" to {
                        "classification" to "NullValueInNonNullableField"
                    }
                }
            )
            "data" to {
                "invalidNullValue" to null
            }
        }
    }
}
