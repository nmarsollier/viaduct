package viaduct.engine.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import viaduct.engine.api.mocks.MockTenantModuleBootstrapper
import viaduct.engine.runtime.fixtures.runFeatureTest
import viaduct.engine.runtime.fixtures.toViaductBuilder
import viaduct.service.runtime.SchemaRegistryBuilder

class OperationValidationTest {
    val testSchema = """
        directive @scope(to: [String!]!) repeatable on OBJECT | INPUT_OBJECT | ENUM | INTERFACE | UNION
        
        type Query @scope(to: ["public","private"]) {
            f1: Int
        }
        
        extend type Query @scope(to: ["private"]) {
            f2: Int
        }
        """

    val viaductBuilder = MockTenantModuleBootstrapper(testSchema) {
        fieldWithValue("Query" to "f1", 1)
        fieldWithValue("Query" to "f2", 2)
    }.toViaductBuilder()
        .withSchemaRegistryBuilder(
            SchemaRegistryBuilder()
                .withFullSchemaFromSdl(testSchema)
                .registerFullSchema("full")
                .registerScopedSchema("public", setOf("public"))
        )

    @Test
    fun `valid full schema query`() {
        viaductBuilder.build().runFeatureTest {
            viaduct.runQuery("full", "{ f1 f2 }")
                .assertJson("""{ "data": {"f1": 1, "f2": 2} }""")
        }
    }

    @Test
    fun `invalid full schema query`() {
        viaductBuilder.build().runFeatureTest {
            val result = viaduct.runQuery("full", "{ f1 f2 f3 }")
            assertEquals(1, result.errors.size)
            assertTrue(result.errors[0].message.contains("FieldUndefined@[f3]"))
        }
    }

    @Test
    fun `invalid scoped schema query`() {
        viaductBuilder.build().runFeatureTest {
            val result = viaduct.runQuery("public", "{ f1 f2 }")
            assertEquals(1, result.errors.size)
            assertTrue(result.errors[0].message.contains("FieldUndefined@[f2]"))
        }
    }
}
