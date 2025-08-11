package viaduct.testapps.hotloading

import java.util.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.graphql.test.assertEquals
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

const val DEFAULT_PUBLIC_SCOPE_ID = "publicScope"
const val DEFAULT_SCHEMA_ID = "publicSchema"

class SchemaHotloadingE2ETest : TestBase(
    setOf(ScopedSchemaInfo(DEFAULT_SCHEMA_ID, setOf(DEFAULT_PUBLIC_SCOPE_ID))),
    ".*(default_schema|old_schema|old_schema_2)\\.graphqls",
    tenantPackageFinder = TestTenantPackageFinder(Tenants),
) {
    private val DEFAULT_NEW_SCHEMA_REGEX = ".*(default|new)_schema\\.graphqls"

    @Test
    fun `basic field resolution without schema hotswap`() {
        // normal field resolution
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                query {
                    field1 {
                      value1
                      value2 {
                        strField
                      }
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "field1" to {
                    "value1" to "tenant1 query field1"
                    "value2" to {
                        "strField" to "tenant1 object2 strField"
                    }
                }
            }
        }

        // field not exist in the schema return validation error
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { nonExistField }"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Validation error (FieldUndefined@[nonExistField]) : Field 'nonExistField' in type 'Query' is undefined"
                    "locations" to arrayOf(
                        {
                            "line" to 1
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
    fun `unchanged field in the new schema resolves as usual`() {
        beginHotSwap()
        // unchanged field resolves as usual
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                query {
                    field1 {
                      value1
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "field1" to {
                    "value1" to "tenant1 query field1"
                }
            }
        }
        endHotSwap()
    }

    @Test
    fun `new nullable field with field resolver in the new schema returns null`() {
        beginHotSwap()
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { nullableField }"
        ).assertEquals {
            "data" to {
                "nullableField" to null
            }
        }
        endHotSwap()
    }

    @Test
    fun `new nullable field without field resolver in the new schema returns null`() {
        beginHotSwap()
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { field1 { fieldAdded } }"
        ).assertEquals {
            "data" to {
                "field1" to {
                    "fieldAdded" to null
                }
            }
        }
        endHotSwap()
    }

    @Test
    fun `adding a nullable field argument should resolve as if no field argument is added`() {
        beginHotSwap()

        // If the new field argument is not passed in from the query, resolve using the old resolver behavior
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { fieldWithArgs(arg1: \"abc\") }",
        ).assertEquals {
            "data" to {
                "fieldWithArgs" to "abc"
            }
        }

        // If the new field argument is passed in from the query, ignore the argument when resolving
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery(${'$'}arg1: String, ${'$'}arg2: String) { fieldWithArgs(arg1: ${'$'}arg1, arg2: ${'$'}arg2) }",
            variables = mapOf("arg1" to "abc", "arg2" to "def")
        ).assertEquals {
            "data" to {
                "fieldWithArgs" to "abc"
            }
        }
        endHotSwap()
    }

    @Test
    fun `add a nullable input object field`() {
        beginHotSwap()

        // If the new input field argument intField is not passed in from query, resolve using the old resolver behavior
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery(${'$'}input: TestInput) { fieldWithInput(input: ${'$'}input) }",
            variables = mapOf("input" to mapOf("strField" to "abc"))
        ).assertEquals {
            "data" to {
                "fieldWithInput" to "tenant1 fieldWithInput abc"
            }
        }

        // If the new field argument is passed in from the query, ignore the argument when resolving
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery(${'$'}input: TestInput) { fieldWithInput(input: ${'$'}input) }",
            variables = mapOf("input" to mapOf("strField" to "abc", "intField" to 123))
        ).assertEquals {
            "data" to {
                "fieldWithInput" to "tenant1 fieldWithInput abc"
            }
        }
        endHotSwap()
    }

    @Test
    fun `add @resolver to an existing field should resolve to the old resolver value`() {
        beginHotSwap()
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { field1 { fieldToBeAddedResolver } }"
        ).assertEquals {
            "data" to {
                "field1" to {
                    "fieldToBeAddedResolver" to "tenant1 query fieldToBeAddedResolver"
                }
            }
        }
        endHotSwap()
    }

    @Test
    fun `removing @resolver on existing field should still resolve using old resolver`() {
        beginHotSwap()
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { field1 { value1 fieldToDropAtResolver } }"
        ).assertEquals {
            "data" to {
                "field1" to {
                    "value1" to "tenant1 query field1"
                    "fieldToDropAtResolver" to "tenant1 object1 fieldToDropAtResolver"
                }
            }
        }
        endHotSwap()
    }

    @Test
    fun `field without code reference removed in the new schema should return request level validation error`() {
        beginHotSwap()
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { field1 { fieldToBeRemoved } }"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Validation error (FieldUndefined@[field1/fieldToBeRemoved]) : Field 'fieldToBeRemoved' in type 'Object1' is undefined"
                    "locations" to arrayOf(
                        {
                            "line" to 1
                            "column" to 18
                        }
                    )
                    "extensions" to {
                        "classification" to "ValidationError"
                    }
                }
            )
            "data" to null
        }
        endHotSwap()
    }

    @Test
    fun `changing scope of field should resolve request using the new scope`() {
        val internalSchemaId = "internalSchema"
        val internalScope = "internalScope"
        beginHotSwap(
            setOf(
                ScopedSchemaInfo(internalSchemaId, setOf(internalScope)),
                ScopedSchemaInfo(DEFAULT_SCHEMA_ID, setOf(DEFAULT_PUBLIC_SCOPE_ID))
            ),
            ".*(default_schema|new_schema_2)\\.graphqls"
        )

        // If querying the field with old scope, return validation error
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { scopedStrField }"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Validation error (FieldUndefined@[scopedStrField]) : Field 'scopedStrField' in type 'Query' is undefined"
                    "locations" to arrayOf(
                        {
                            "line" to 1
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

        // If querying the field with new scope, query succeeds
        execute(
            schemaId = internalSchemaId,
            query = "query { scopedStrField }"
        ).assertEquals {
            "data" to {
                "scopedStrField" to "tenant2 query scopedStrField"
            }
        }
        endHotSwap()
    }

    @Test
    fun `moving field from one tenant module to another should resolve using old resolver`() {
        beginHotSwap()
        // Since we concatenate all the schema files into _one_ new file, ownership of fields is not directly encoded
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { field1 { fieldAnotherModule } }"
        ).assertEquals {
            "data" to {
                "field1" to {
                    "fieldAnotherModule" to "tenant1 query fieldAnotherModule"
                }
            }
        }
        endHotSwap()
    }

    @Test
    fun `adding an enum value in output and input types`() {
        beginHotSwap(".*(default_schema|new_schema_2)\\.graphqls")

        // Does not affect query resolution if enum is used as output type
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { objField3 { field1 } }"
        ).assertEquals {
            "data" to {
                "objField3" to {
                    "field1" to "SomeEnum.VALUE1"
                }
            }
        }

        // Does not affect query resolution if enum is used as input type but the new value is not passed in
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery(${'$'}input: SomeEnum) { objField3 { field2(enumInput: ${'$'}input) } }",
            variables = mapOf("input" to "VALUE1")
        ).assertEquals {
            "data" to {
                "objField3" to {
                    "field2" to "SomeEnum.VALUE1"
                }
            }
        }

        // Query fails if enum is used as input type and the new value is passed in
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery(${'$'}input: SomeEnum) { objField3 { field2(enumInput: ${'$'}input) } }",
            variables = mapOf("input" to "NEW_VALUE")
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "viaduct.api.ViaductFrameworkException: InputLikeBase.get failed for Object5_Field2_Arguments.enumInput " +
                        "(java.lang.IllegalArgumentException: No enum constant viaduct.api.grts.SomeEnum.NEW_VALUE)"
                    "locations" to arrayOf(
                        {
                            "line" to 1
                            "column" to 49
                        }
                    )
                    "path" to listOf(
                        "objField3",
                        "field2"
                    )
                    "extensions" to {
                        "fieldName" to "field2"
                        "parentType" to "Object5"
                        "operationName" to "TestQuery"
                        "classification" to "DataFetchingException"
                    }
                }
            )
            "data" to {
                "objField3" to {
                    "field2" to null
                }
            }
        }
    }

    @Test
    fun `adding a union member does not affect query resolution`() {
        beginHotSwap(".*(default_schema|new_schema_2)\\.graphqls")
        // When we deploy code change to also return Dog type for pets, the query may fail if the client throws on unknown __typename
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { pets { __typename ...on Cat { name } } }",
        ).assertEquals {
            "data" to {
                "pets" to arrayOf(
                    {
                        "__typename" to "Cat"
                        "name" to "Fluffy"
                    }
                )
            }
        }
        endHotSwap()
    }

    @Test
    fun `adding nullable field in node type`() {
        beginHotSwap()
        val id = Base64.getEncoder().encodeToString("TestNode1:testId".toByteArray())
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { node(id: \"testId\") { id key value } }"
        ).assertEquals {
            "data" to {
                "node" to {
                    "id" to id
                    "key" to "testNodeKey"
                    "value" to null
                }
            }
        }
        endHotSwap()
    }

    @Test
    fun `adding nullable field in node reference type`() {
        beginHotSwap()
        val id = Base64.getEncoder().encodeToString("TestNode1:testId".toByteArray())
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { nodeReference(id: \"testId\") { id key value } }"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "viaduct.engine.api.UnsetSelectionException: Attempted to access field TestNode1.value but it was not set: " +
                        "Please set a value for value using the builder for TestNode1"
                    "locations" to arrayOf(
                        {
                            "line" to 1
                            "column" to 46
                        }
                    )
                    "path" to listOf(
                        "nodeReference",
                        "value"
                    )
                    "extensions" to {
                        "fieldName" to "value"
                        "parentType" to "TestNode1"
                        "classification" to "DataFetchingException"
                    }
                }
            )
            "data" to {
                "nodeReference" to {
                    "id" to id
                    "key" to "key"
                    "value" to null
                }
            }
        }
        endHotSwap()
    }

    // We provide the backwards incompatible schema changes test cases here to ensure that in case such changes get deployed, our server won't crash.

    @Test
    fun `new non-nullable field in the new schema should return error and bubble up to the closest nullable parent`() {
        beginHotSwap()
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { field1 { value2 { nonNullStrField } } }"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "The field at path '/field1/value2/nonNullStrField' was declared as a non null type, " +
                        "but the code involved in retrieving data has wrongly returned a null value.  " +
                        "The graphql specification requires that the parent field be set to null, " +
                        "or if that is non nullable that it bubble up null to its parent and so on. " +
                        "The non-nullable type is 'String' within parent type 'Object2'"
                    "path" to listOf(
                        "field1",
                        "value2",
                        "nonNullStrField"
                    )
                    "extensions" to {
                        "classification" to "NullValueInNonNullableField"
                    }
                }
            )
            "data" to {
                "field1" to null
            }
        }
        endHotSwap()
    }

    @Test
    fun `update input field from non-nullable to nullable`() {
        beginHotSwap()

        // If field input is not passed in, return request level NullPointerException (NPE)
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { fieldWithInput }",
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "java.lang.NullPointerException: Cannot invoke \"viaduct.api.grts.TestInput.getStrField()\"" +
                        " because the return value of \"viaduct.api.grts.Query_FieldWithInput_Arguments.getInput()\" is null"
                    "locations" to arrayOf(
                        {
                            "line" to 1
                            "column" to 9
                        }
                    )
                    "path" to listOf("fieldWithInput")
                    "extensions" to {
                        "fieldName" to "fieldWithInput"
                        "parentType" to "Query"
                        "classification" to "DataFetchingException"
                    }
                }
            )
            "data" to null
        }

        // If field input is passed in as null, return request level NPE
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery(${'$'}input: TestInput) { fieldWithInput(input: ${'$'}input) }",
            variables = mapOf("input" to null)
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "java.lang.NullPointerException: " +
                        "Cannot invoke \"viaduct.api.grts.TestInput.getStrField()\" because the return value of " +
                        "\"viaduct.api.grts.Query_FieldWithInput_Arguments.getInput()\" is null"
                    "locations" to arrayOf(
                        {
                            "line" to 1
                            "column" to 38
                        }
                    )
                    "path" to listOf("fieldWithInput")
                    "extensions" to {
                        "fieldName" to "fieldWithInput"
                        "parentType" to "Query"
                        "operationName" to "TestQuery"
                        "classification" to "DataFetchingException"
                    }
                }
            )
            "data" to null
        }

        // If input object field TestInput.strField is not passed in, query succeeds
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery(${'$'}input: TestInput) { fieldWithInput(input: ${'$'}input) }",
            variables = mapOf("input" to mapOf("intField" to 123))
        ).assertEquals {
            "data" to { "fieldWithInput" to "tenant1 fieldWithInput null" }
        }

        // If input object field TestInput.strField passed in as null, query succeeds
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery(${'$'}input: TestInput) { fieldWithInput(input: ${'$'}input) }",
            variables = mapOf("input" to mapOf("strField" to null, "intField" to 123))
        ).assertEquals {
            "data" to { "fieldWithInput" to "tenant1 fieldWithInput null" }
        }

        endHotSwap()
    }

    @Test
    fun `update output field from non-nullable to nullable`() {
        beginHotSwap()

        // If query failed, then bubble up failure to the nullable field
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { fieldWithInput2 }",
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "java.lang.NullPointerException: Cannot invoke \"viaduct.api.grts.TestInput.getStrField()\"" +
                        " because the return value of \"viaduct.api.grts.Query_FieldWithInput2_Arguments.getInput()\" is null"
                    "locations" to arrayOf(
                        {
                            "line" to 1
                            "column" to 9
                        }
                    )
                    "path" to listOf("fieldWithInput2")
                    "extensions" to {
                        "fieldName" to "fieldWithInput2"
                        "parentType" to "Query"
                        "classification" to "DataFetchingException"
                    }
                }
            )
            "data" to {
                "fieldWithInput2" to null
            }
        }

        // If non-nullable field resolves to null, query succeeds although the resolver signature is of non-null type
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery(${'$'}input: TestInput) { fieldWithInput2(input: ${'$'}input) }",
            variables = mapOf("input" to mapOf("intField" to 123))
        ).assertEquals {
            "data" to { "fieldWithInput2" to null }
        }
        endHotSwap()
    }

    @Test
    fun `output field type changed from String to Int returns DataFetchingException`() {
        beginHotSwap()
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { field1 { value1 } fieldToBeUpdated }"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Can't serialize value (/fieldToBeUpdated) : Expected a value that can be converted to type 'Int' but it was a 'String'"
                    "path" to listOf(
                        "fieldToBeUpdated"
                    )
                    "extensions" to {
                        "classification" to "DataFetchingException"
                    }
                }
            )
            "data" to {
                "field1" to {
                    "value1" to "tenant1 query field1"
                }
                "fieldToBeUpdated" to null
            }
        }
        endHotSwap()
    }

    @Test
    fun `resolver returning an enum value that has been removed should return field level serialization error`() {
        beginHotSwap()
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { field1 { value2 { enumField } } }"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Can't serialize value (/field1/value2/enumField) : Invalid input for enum 'TestEnum'. Unknown value 'VALUE2'"
                    "path" to listOf("field1", "value2", "enumField")
                    "extensions" to {
                        "classification" to "DataFetchingException"
                    }
                }
            )
            "data" to {
                "field1" to {
                    "value2" to {
                        "enumField" to null
                    }
                }
            }
        }
        endHotSwap()
    }

    @Test
    fun `remove field that has field resolver should return request validation error`() {
        beginHotSwap(".*(default_schema|new_schema_2)\\.graphqls")
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { objField { field2 } }"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Validation error (FieldUndefined@[objField/field2]) : Field 'field2' in type 'Object3' is undefined"
                    "locations" to arrayOf(
                        {
                            "line" to 1
                            "column" to 20
                        }
                    )
                    "extensions" to {
                        "classification" to "ValidationError"
                    }
                }
            )
            "data" to null
        }
        endHotSwap()
    }

    @Test
    fun `removing field that has code reference`() {
        beginHotSwap(".*(default_schema|new_schema_2)\\.graphqls")
        // returns validation error is the removed field is requested
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { objField { field1 field3 } }"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "Validation error (FieldUndefined@[objField/field3]) : Field 'field3' in type 'Object3' is undefined"
                    "locations" to arrayOf(
                        {
                            "line" to 1
                            "column" to 27
                        }
                    )
                    "extensions" to {
                        "classification" to "ValidationError"
                    }
                }
            )
            "data" to null
        }
        // returns DataFetchingException if the removed field is not requested, since the resolver still uses it in the GRT builder
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query { objField { field1 field4 } }"
        ).assertEquals {
            "errors" to arrayOf(
                {
                    "message" to "viaduct.api.ViaductFrameworkException: ObjectBase.Builder.putInternal failed (java.lang.NullPointerException: " +
                        "Cannot invoke \"graphql.schema.GraphQLFieldDefinition.getType()\" because \"field\" is null)"
                    "locations" to arrayOf(
                        {
                            "line" to 1
                            "column" to 9
                        }
                    )
                    "path" to listOf("objField")
                    "extensions" to {
                        "fieldName" to "objField"
                        "parentType" to "Query"
                        "classification" to "DataFetchingException"
                    }
                }
            )
            "data" to {
                "objField" to null
            }
        }
        endHotSwap()
    }

    @Test
    fun `removing field that is referenced in RSS should not crash`() {
        // TODO: currently throw exception. Will fail it gracefully in following PR.
        val exception = assertThrows<AssertionError> {
            beginHotSwap(".*(default_schema|new_schema_2)\\.graphqls")
            execute(
                schemaId = DEFAULT_SCHEMA_ID,
                query = "query { objField2 { field2 } }"
            ).assertEquals {
                "data" to {
                    "objField2" to null
                }
            }
            endHotSwap()
        }

        val commonSuffix = " <{errors=[{message=viaduct.api.ViaductFrameworkException: ObjectBase.Builder.putInternal failed"
        assertTrue(
            listOf(
                "Expected <{data={objField2=null}}>, actual$commonSuffix",
                "expected: <{data={objField2=null}}> but was:$commonSuffix",
            ).any {
                exception.message!!.startsWith(it)
            },
            exception.message
        )
    }

    private fun beginHotSwap(newSchemaRegex: String? = null) {
        beginHotSwap(setOf(ScopedSchemaInfo(DEFAULT_SCHEMA_ID, setOf(DEFAULT_PUBLIC_SCOPE_ID))), newSchemaRegex ?: DEFAULT_NEW_SCHEMA_REGEX)
    }
}
