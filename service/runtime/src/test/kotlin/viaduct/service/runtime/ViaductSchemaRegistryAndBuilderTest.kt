package viaduct.service.runtime

import graphql.schema.idl.SchemaParser
import graphql.schema.idl.UnExecutableSchemaGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import viaduct.engine.api.ViaductSchema
import viaduct.engine.api.mocks.mkCoroutineInterop
import viaduct.engine.api.mocks.mkExecutionStrategy
import viaduct.engine.api.mocks.mkInstrumentation

class ViaductSchemaRegistryAndBuilderTest {
    private val mockCoroutineInterop = mkCoroutineInterop()
    private val mockExecutionStrategy = mkExecutionStrategy()
    private val mockInstrumentation = mkInstrumentation()

    fun checkResults(
        registry: ViaductSchemaRegistry,
        schemaId: String
    ): ViaductSchema {
        val actualSchema = registry.getSchema(schemaId)
        assertNotNull(actualSchema)
        assertEquals("Query", actualSchema?.schema?.queryType?.name)

        val engine = registry.getEngine(schemaId)
        assertNotNull(engine?.graphQLSchema)
        assertEquals(actualSchema, ViaductSchema(requireNotNull(engine?.graphQLSchema)))

        val actualEngine = registry.getEngine(schemaId)
        assertNotNull(actualEngine?.graphQLSchema)
        assertEquals(actualSchema, ViaductSchema(requireNotNull(actualEngine?.graphQLSchema)))

        return actualSchema!!
    }

    @Test
    fun `test successful schema creation from SDL string`() {
        val validSchema = """
            type Query {
                hello: String
            }
        """.trimIndent()

        val builder = ViaductSchemaRegistryBuilder()
            .withFullSchemaFromSdl(validSchema)
            .registerFullSchema("testSchema")

        val registry = builder.build(mockCoroutineInterop)
        registry.registerSchema(mockInstrumentation, mockExecutionStrategy, mockExecutionStrategy, mockExecutionStrategy)

        checkResults(registry, "testSchema")
    }

    @Test
    fun `test successful scoped schema registration`() {
        val fullSchema = """
            directive @scope(to: [String!]!) repeatable on OBJECT | INPUT_OBJECT | ENUM | INTERFACE | UNION

            type Query @scope(to: ["public"]) {
                hello: String
            }

            type PrivateQuery @scope(to: ["private"]) {
                privateField: String
            }
        """.trimIndent()

        val builder = ViaductSchemaRegistryBuilder()
            .withFullSchemaFromSdl(fullSchema)
            .registerScopedSchema("publicSchema", setOf("public"))

        val registry = builder.build(mockCoroutineInterop)
        registry.registerSchema(mockInstrumentation, mockExecutionStrategy, mockExecutionStrategy, mockExecutionStrategy)

        val actualSchema = checkResults(registry, "publicSchema")
        assertNotNull(actualSchema.schema.queryType!!.getField("hello"))
        assertNull(actualSchema.schema.queryType!!.getField("privateField"))
        assertNotEquals(registry.getFullSchema(), actualSchema)
    }

    @Test
    fun `test async schema registration with lazy loading`() {
        val fullSchema = """
            type Query {
                hello: String
            }
        """.trimIndent()

        var schemaComputeCalled = 0
        val schemaComputeBlock = {
            schemaComputeCalled++
            makeTestSchema(fullSchema)
        }

        val builder = ViaductSchemaRegistryBuilder()
            .withFullSchemaFromSdl(fullSchema)
            .registerSchema("asyncSchema", schemaComputeBlock, lazy = true)

        val registry = builder.build(mockCoroutineInterop)
        registry.registerSchema(mockInstrumentation, mockExecutionStrategy, mockExecutionStrategy, mockExecutionStrategy)

        // Schema compute block should not be called during build with lazy=true
        assertEquals(0, schemaComputeCalled, "Schema compute block should not be called during build when lazy=true")

        // Accessing the schema should trigger computation
        checkResults(registry, "asyncSchema")
        assertEquals(1, schemaComputeCalled, "Schema compute block should be called once when accessing lazy schema")
    }

    @Test
    fun `test async schema registration with eager loading`() {
        val fullSchema = """
            type Query {
                hello: String
            }
        """.trimIndent()

        var schemaComputeCalled = 0
        val schemaComputeBlock = {
            schemaComputeCalled++
            makeTestSchema(fullSchema)
        }

        val builder = ViaductSchemaRegistryBuilder()
            .withFullSchemaFromSdl(fullSchema)
            .registerSchema("asyncSchema", schemaComputeBlock, lazy = false)

        val registry = builder.build(mockCoroutineInterop)
        registry.registerSchema(mockInstrumentation, mockExecutionStrategy, mockExecutionStrategy, mockExecutionStrategy)

        // Schema compute block should be called during build with lazy=false
        assertEquals(1, schemaComputeCalled, "Schema compute block should be called when lazy=false")

        checkResults(registry, "asyncSchema")
        assertEquals(1, schemaComputeCalled, "Schema compute block should be called when lazy=false")
    }

    @Test
    fun `test withFullSchema with predefined schema`() {
        val predefinedSchema = makeTestSchema()

        val builder = ViaductSchemaRegistryBuilder()
            .withFullSchema(predefinedSchema)
            .registerFullSchema("testSchema")

        val registry = builder.build(mockCoroutineInterop)
        registry.registerSchema(mockInstrumentation, mockExecutionStrategy, mockExecutionStrategy, mockExecutionStrategy)

        assertEquals(predefinedSchema, registry.getSchema("testSchema"))
    }

    @Test
    fun `schema scopes extension finds all scopes`() {
        val allScopes = makeTestSchema(
            """
                directive @scope(to: [String!]!) repeatable on OBJECT | INPUT_OBJECT | ENUM | INTERFACE | UNION
                type Query @scope(to: ["*"]) {
                  _: String @deprecated
                }

                extend type Query @scope(to: ["scope1"]) {
                  field1: String
                }

                extend type Query @scope(to: ["scope2", "scope3"]) {
                  field2: String
                }
            """.trimIndent()
        ).scopes()

        assertEquals(setOf("scope1", "scope2", "scope3"), allScopes)
    }
}

internal fun makeTestSchema(sdl: String): ViaductSchema = ViaductSchema(UnExecutableSchemaGenerator.makeUnExecutableSchema(SchemaParser().parse(sdl)))

internal fun makeTestSchema(): ViaductSchema {
    return ViaductSchema(
        UnExecutableSchemaGenerator.makeUnExecutableSchema(
            SchemaParser().parse(
                """
                    type Query {
                        hello: String
                    }
                """.trimIndent()
            )
        )
    )
}
