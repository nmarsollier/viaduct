package viaduct.cli.validation.schema

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ViaductSchemaValidatorCLITest {
    @Test
    fun `valid single schema returns no errors`(
        @TempDir tempDir: File
    ) {
        val schema = """
            type Query {
              ping: String
            }
        """.trimIndent()
        val file = tempDir.resolve("schema.graphqls")
        file.writeText(schema)

        val errors = validateSchema(file)

        assertTrue(errors.isEmpty(), "Expected no validation errors: $errors")
    }

    @Test
    fun `valid multi file directory schema returns no errors`(
        @TempDir tempDir: File
    ) {
        tempDir.resolve("Query.graphqls").writeText(
            """
            type Query {
              user(id: ID!): User
            }
            """.trimIndent()
        )
        tempDir.resolve("User.graphqls").writeText(
            """
            type User {
              id: ID!
              name: String
            }
            """.trimIndent()
        )

        val errors = validateSchema(tempDir)

        assertTrue(errors.isEmpty(), "Expected no validation errors: $errors")
    }

    @Test
    fun `invalid schema produces errors`(
        @TempDir tempDir: File
    ) {
        tempDir.resolve("Broken.graphqls").writeText(
            """
            type Query {
              user: User
            }
            # Missing type User
            """.trimIndent()
        )

        val errors = validateSchema(tempDir)

        assertFalse(errors.isEmpty(), "Expected validation errors")
        assertTrue(errors.any { it.message?.contains("User") == true })
    }

    @Test
    fun `empty directory should throw error - Schema content is empty or blank`(
        @TempDir tempDir: File
    ) {
        val errors = validateSchema(tempDir)
        assertTrue(errors.isNotEmpty(), "Expected validation errors for empty directory")
        assertTrue(errors.any { it.message?.contains("Schema content is empty or blank") == true })
    }

    @Test
    fun `loadSchemasFromDirectory returns concatenated SDL`(
        @TempDir tempDir: File
    ) {
        tempDir.resolve("A.graphqls").writeText("type A { id: ID! }")
        tempDir.resolve("B.graphqls").writeText("type B { id: ID! }")

        val combined = readSchemaFromDirectory(tempDir)

        assertNotNull(combined)
        assertTrue(combined.contains("type A"))
        assertTrue(combined.contains("type B"))
    }
}
