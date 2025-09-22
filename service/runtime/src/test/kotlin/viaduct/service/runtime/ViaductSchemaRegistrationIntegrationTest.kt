@file:Suppress("ForbiddenImport")

package viaduct.service.runtime

import graphql.ExecutionResult
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import viaduct.engine.api.ViaductSchema
import viaduct.graphql.utils.DefaultSchemaProvider
import viaduct.service.api.ExecutionInput
import viaduct.service.api.spi.Flag
import viaduct.service.api.spi.FlagManager

/**
 * Integration tests for Viaduct schema registration functionality.
 *
 * These tests validate:
 * - Different schema registration APIs (SDL, resources, full schema)
 * - Schema ID routing and error handling
 * - Scoped schema functionality
 * - Multi-schema registration and isolation
 */
@ExperimentalCoroutinesApi
class ViaductSchemaRegistrationIntegrationTest {
    private lateinit var subject: StandardViaduct
    private lateinit var viaductSchemaRegistryBuilder: ViaductSchemaRegistryBuilder

    val flagManager = object : FlagManager {
        override fun isEnabled(flag: Flag) = true
    }

    val wiring = RuntimeWiring.MOCKED_WIRING

    // Test equivalent to RegisterSchemaFromResourcesTest - basic scoped schema functionality
    @Test
    fun `Resolve SCOPE1 and SCOPE2 fields with scoped schemas`() =
        runBlocking {
            val sdl = """
            type TestScope1Object @scope(to: ["SCOPE1"]) {
              strValue: String!
            }

            extend type Query @scope(to: ["SCOPE1"]) {
              scope1Value: TestScope1Object
            }

            type TestScope2Object @scope(to: ["SCOPE2"]) {
              strValue: String!
            }

            extend type Query @scope(to: ["SCOPE2"]) {
              scope2Value: TestScope2Object
            }
        """

            viaductSchemaRegistryBuilder = ViaductSchemaRegistryBuilder()
                .withFullSchemaFromSdl(sdl)
                .registerScopedSchema("SCHEMA_ID_1", setOf("SCOPE1"))
                .registerScopedSchema("SCHEMA_ID_2", setOf("SCOPE2"))

            subject = StandardViaduct.Builder()
                .withFlagManager(flagManager)
                .withNoTenantAPIBootstrapper()
                .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
                .build()

            // Test SCOPE1 query with SCHEMA_ID_1
            val query1 = """
            query {
                scope1Value {
                    strValue
                }
            }
            """.trimIndent()
            val executionInput1 = ExecutionInput(query1, "SCHEMA_ID_1", object {})
            val result1 = subject.executeAsync(executionInput1).await()

            val expected1 = ExecutionResult.newExecutionResult()
                .data(mapOf("scope1Value" to null)) // Mocked wiring returns null
                .build()

            assertEquals(expected1.toSpecification(), result1.toSpecification())

            // Test SCOPE2 query with SCHEMA_ID_2
            val query2 = """
            query {
                scope2Value {
                    strValue
                }
            }
            """.trimIndent()
            val executionInput2 = ExecutionInput(query2, "SCHEMA_ID_2", object {})
            val result2 = subject.executeAsync(executionInput2).await()

            val expected2 = ExecutionResult.newExecutionResult()
                .data(mapOf("scope2Value" to null)) // Mocked wiring returns null
                .build()

            assertEquals(expected2.toSpecification(), result2.toSpecification())
        }

    // Test equivalent to RegisterSchemaFromResources1Test/2Test - scoped schema isolation
    @Test
    fun `Scoped schema excludes fields from other scopes`() =
        runBlocking {
            val sdl = """
            type TestScope1Object @scope(to: ["SCOPE1"]) {
              strValue: String!
            }

            extend type Query @scope(to: ["SCOPE1"]) {
              scope1Value: TestScope1Object
            }

            type TestScope2Object @scope(to: ["SCOPE2"]) {
              strValue: String!
            }

            extend type Query @scope(to: ["SCOPE2"]) {
              scope2Value: TestScope2Object
            }
        """

            viaductSchemaRegistryBuilder = ViaductSchemaRegistryBuilder()
                .withFullSchemaFromSdl(sdl)
                .registerScopedSchema("SCOPE1_ONLY", setOf("SCOPE1"))
                .registerScopedSchema("SCOPE2_ONLY", setOf("SCOPE2"))

            subject = StandardViaduct.Builder()
                .withFlagManager(flagManager)
                .withNoTenantAPIBootstrapper()
                .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
                .build()

            // Try to query SCOPE2 field with SCOPE1_ONLY schema - should fail validation
            val query = """
            query {
                scope2Value {
                    strValue
                }
            }
            """.trimIndent()
            val executionInput = ExecutionInput(query, "SCOPE1_ONLY", object {})
            val result = subject.executeAsync(executionInput).await()

            // Should get validation error because scope2Value is not available in SCOPE1_ONLY schema
            // The original test validated that scoped schemas properly exclude fields from other scopes
            assertEquals(1, result.errors.size)
            assert(result.errors[0].message.contains("Field 'scope2Value' in type 'Query' is undefined")) {
                "Expected validation error message, but got: ${result.errors[0].message}"
            }
            // Verify this is a validation error (GraphQL validation happens before execution)
            assertNull(result.getData<Any>(), "Data should be null when validation fails")

            // Test the reverse - SCOPE2_ONLY should fail when querying SCOPE1 fields
            val reverseQuery = """
            query {
                scope1Value {
                    strValue
                }
            }
            """.trimIndent()
            val reverseExecutionInput = ExecutionInput(reverseQuery, "SCOPE2_ONLY", object {})
            val reverseResult = subject.executeAsync(reverseExecutionInput).await()

            assertEquals(1, reverseResult.errors.size)
            assert(reverseResult.errors[0].message.contains("Field 'scope1Value' in type 'Query' is undefined")) {
                "Expected validation error for scope1Value in SCOPE2_ONLY schema, but got: ${reverseResult.errors[0].message}"
            }
            assertNull(reverseResult.getData<Any>(), "Data should be null when validation fails")
        }

    // Test equivalent to RegisterSchemaFromResources3Test - missing schema ID error
    @Test
    fun `Fails to execute query with unregistered schema ID`() =
        runBlocking {
            val sdl = """
            type TestScope1Object @scope(to: ["SCOPE1"]) {
              strValue: String!
            }

            extend type Query @scope(to: ["SCOPE1"]) {
              scope1Value: TestScope1Object
            }
        """

            viaductSchemaRegistryBuilder = ViaductSchemaRegistryBuilder()
                .withFullSchemaFromSdl(sdl)
                .registerScopedSchema("SCHEMA_ID_1", setOf("SCOPE1"))
            // Note: not registering SCHEMA_ID_2

            subject = StandardViaduct.Builder()
                .withFlagManager(flagManager)
                .withNoTenantAPIBootstrapper()
                .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
                .build()

            val query = """
            query {
                scope1Value {
                    strValue
                }
            }
            """.trimIndent()
            val executionInput = ExecutionInput(query, "SCHEMA_ID_2", object {}) // Unregistered schema ID
            val result = subject.executeAsync(executionInput).await()

            // The original test expected this to fail with "Schema not found" error
            assertEquals(1, result.errors.size)
            assertEquals("Schema not found for schemaId=SCHEMA_ID_2", result.errors[0].message)
            assertNull(result.getData<Any>())
        }

    // Test equivalent to RegisterFullSchemaTest - custom full schema registration
    @Test
    fun `Register and query custom full schema`() =
        runBlocking {
            val sdl = """
            type TestScope1Object {
              strValue: String!
            }

            extend type Query {
              scope1Value: TestScope1Object
            }

            type TestScope2Object {
              strValue: String!
            }

            extend type Query {
              scope2Value: TestScope2Object
            }
        """

            // Create ViaductSchema object first, then use withFullSchema()
            val schema = mkSchema(sdl)
            viaductSchemaRegistryBuilder = ViaductSchemaRegistryBuilder()
                .withFullSchema(schema)
                .registerFullSchema("FULL_SCHEMA")

            subject = StandardViaduct.Builder()
                .withFlagManager(flagManager)
                .withNoTenantAPIBootstrapper()
                .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
                .build()

            val query = """
            query {
                scope1Value {
                    strValue
                }
            }
            """.trimIndent()
            val executionInput = ExecutionInput(query, "FULL_SCHEMA", object {})
            val result = subject.executeAsync(executionInput).await()

            val expected = ExecutionResult.newExecutionResult()
                .data(mapOf("scope1Value" to null)) // Mocked wiring returns null
                .build()

            assertEquals(expected.toSpecification(), result.toSpecification())
        }

    // Test equivalent to RegisterSchemaFromSdlTest - SDL-based registration
    @Test
    fun `Register schema from SDL string`() =
        runBlocking {
            val fullSdl = """
            type TestScope1Object {
              strValue: String!
            }

            extend type Query {
              scope1Value: TestScope1Object
            }

            type TestScope2Object {
              strValue: String!
            }

            extend type Query {
              scope2Value: TestScope2Object
            }
        """

            val publicSdl = """
            type TestScope1Object {
              strValue: String!
            }

            extend type Query {
              scope1Value: TestScope1Object
            }
        """

            viaductSchemaRegistryBuilder = ViaductSchemaRegistryBuilder()
                .withFullSchemaFromSdl(fullSdl)
                .registerSchemaFromSdl("PUBLIC", publicSdl)

            subject = StandardViaduct.Builder()
                .withFlagManager(flagManager)
                .withNoTenantAPIBootstrapper()
                .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
                .build()

            // Test successful query
            val query1 = """
            query {
                scope1Value {
                    strValue
                }
            }
            """.trimIndent()
            val executionInput1 = ExecutionInput(query1, "PUBLIC", object {})
            val result1 = subject.executeAsync(executionInput1).await()

            val expected1 = ExecutionResult.newExecutionResult()
                .data(mapOf("scope1Value" to null)) // Mocked wiring returns null
                .build()

            assertEquals(expected1.toSpecification(), result1.toSpecification())

            // Test query with unregistered schema ID fails
            val executionInput2 = ExecutionInput(query1, "UNREGISTERED", object {})
            val result2 = subject.executeAsync(executionInput2).await()

            assertEquals(1, result2.errors.size)
            assertEquals("Schema not found for schemaId=UNREGISTERED", result2.errors[0].message)
            assertNull(result2.getData<Any>())
        }

    // Test multiple schema registration methods work together
    @Test
    fun `Multiple schema registration methods coexist`() =
        runBlocking {
            val sdl = """
            type TestObject @scope(to: ["SCOPE1", "SCOPE2"]) {
              value: String!
            }

            extend type Query @scope(to: ["SCOPE1"]) {
              field1: TestObject
            }

            extend type Query @scope(to: ["SCOPE2"]) {
              field2: TestObject
            }
        """

            viaductSchemaRegistryBuilder = ViaductSchemaRegistryBuilder()
                .withFullSchemaFromSdl(sdl)
                .registerFullSchema("FULL")
                .registerScopedSchema("SCOPED_1", setOf("SCOPE1"))
                .registerScopedSchema("SCOPED_2", setOf("SCOPE2"))
                .registerSchemaFromSdl(
                    "CUSTOM",
                    """
                type CustomObject {
                  customField: String!
                }
                extend type Query {
                  custom: CustomObject
                }
            """
                )

            subject = StandardViaduct.Builder()
                .withFlagManager(flagManager)
                .withNoTenantAPIBootstrapper()
                .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
                .build()

            // Verify all schemas are registered
            assertNotNull(subject.viaductSchemaRegistry.getSchema("FULL"), "FULL schema should be registered")
            assertNotNull(subject.viaductSchemaRegistry.getSchema("SCOPED_1"), "SCOPED_1 schema should be registered")
            assertNotNull(subject.viaductSchemaRegistry.getSchema("SCOPED_2"), "SCOPED_2 schema should be registered")
            assertNotNull(subject.viaductSchemaRegistry.getSchema("CUSTOM"), "CUSTOM schema should be registered")

            // Verify FULL schema has both field1 and field2
            val fullSchema = subject.viaductSchemaRegistry.getSchema("FULL")!!
            val fullQueryType = fullSchema.schema.queryType
            assertNotNull(fullQueryType.getFieldDefinition("field1"), "FULL schema should have field1")
            assertNotNull(fullQueryType.getFieldDefinition("field2"), "FULL schema should have field2")

            // Verify SCOPED_1 has only field1
            val scoped1Schema = subject.viaductSchemaRegistry.getSchema("SCOPED_1")!!
            val scoped1QueryType = scoped1Schema.schema.queryType
            assertNotNull(scoped1QueryType.getFieldDefinition("field1"), "SCOPED_1 schema should have field1")
            assertNull(scoped1QueryType.getFieldDefinition("field2"), "SCOPED_1 schema should not have field2")

            // Verify SCOPED_2 has only field2
            val scoped2Schema = subject.viaductSchemaRegistry.getSchema("SCOPED_2")!!
            val scoped2QueryType = scoped2Schema.schema.queryType
            assertNotNull(scoped2QueryType.getFieldDefinition("field2"), "SCOPED_2 schema should have field2")
            assertNull(scoped2QueryType.getFieldDefinition("field1"), "SCOPED_2 schema should not have field1")

            // Verify CUSTOM schema has custom field
            val customSchema = subject.viaductSchemaRegistry.getSchema("CUSTOM")!!
            val customQueryType = customSchema.schema.queryType
            assertNotNull(customQueryType.getFieldDefinition("custom"), "CUSTOM schema should have custom field")
            assertNull(customQueryType.getFieldDefinition("field1"), "CUSTOM schema should not have field1")
            assertNull(customQueryType.getFieldDefinition("field2"), "CUSTOM schema should not have field2")
        }
}

/**
 * Helper function to create a ViaductSchema from SDL string, similar to the original test.
 */
private fun mkSchema(sdl: String): ViaductSchema =
    ViaductSchema(
        SchemaGenerator().makeExecutableSchema(
            SchemaParser().parse(sdl).apply {
                DefaultSchemaProvider.addDefaults(this)
            },
            RuntimeWiring.MOCKED_WIRING
        )
    )
