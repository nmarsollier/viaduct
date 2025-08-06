package viaduct.tenant.codegen.cli

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import viaduct.tenant.codegen.bytecode.CodeGenArgs
import viaduct.tenant.codegen.bytecode.GRTClassFilesBuilderBase

class SchemaObjectsBytecodeTest {
    @TempDir
    private lateinit var tempDir: File
    private lateinit var schemaFile: File
    private lateinit var generatedDir: File
    private lateinit var outputArchive: File
    private lateinit var pkgFile: File
    private lateinit var mockGRTBuilder: GRTClassFilesBuilderBase

    @BeforeEach
    fun setup() {
        schemaFile = File(tempDir, "schema.graphql").apply {
            createNewFile()
            writeText("type Query { hello: String }")
        }
        generatedDir = File(tempDir, "generated").apply { mkdirs() }
        outputArchive = File(tempDir, "output.zip")
        pkgFile = File(tempDir, "package.txt").apply {
            createNewFile()
            writeText("com.test.package")
        }

        mockGRTBuilder = mockk(relaxed = true)
        mockkObject(GRTClassFilesBuilderBase.Companion)
        every { GRTClassFilesBuilderBase.builderFrom(any()) } returns mockGRTBuilder
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test successful execution with required options`() {
        val argsSlot = slot<CodeGenArgs>()
        every { GRTClassFilesBuilderBase.builderFrom(capture(argsSlot)) } returns mockGRTBuilder

        SchemaObjectsBytecode().main(
            listOf("--generated_directory", generatedDir.absolutePath) +
                listOf("--schema_files", schemaFile.absolutePath) +
                listOf("--bytecode_worker_number", "0") +
                listOf("--bytecode_worker_count", "1")
        )

        verify { GRTClassFilesBuilderBase.builderFrom(any()) }
        with(argsSlot.captured) {
            assertEquals("com.airbnb.viaduct.schema.generated", pkgForGeneratedClasses)
            assertEquals(0, workerNumber)
            assertEquals(1, workerCount)
            assertFalse(includeIneligibleTypesForTestingOnly)
        }
    }

    @Test
    fun `test with optional parameters`() {
        val argsSlot = slot<CodeGenArgs>()
        every { GRTClassFilesBuilderBase.builderFrom(capture(argsSlot)) } returns mockGRTBuilder

        SchemaObjectsBytecode().main(
            listOf("--generated_directory", generatedDir.absolutePath) +
                listOf("--schema_files", schemaFile.absolutePath) +
                listOf("--bytecode_worker_number", "0") +
                listOf("--bytecode_worker_count", "1") +
                listOf("--module_name", "testModule") +
                listOf("--pkg_for_generated_classes", "com.test.generated") +
                listOf("--include_ineligible_for_testing_only")
        )

        verify { GRTClassFilesBuilderBase.builderFrom(any()) }
        with(argsSlot.captured) {
            assertEquals("testModule", moduleName)
            assertEquals("com.test.generated", pkgForGeneratedClasses)
            assertTrue(includeIneligibleTypesForTestingOnly)
        }
    }

    @Test
    fun `test with package from file`() {
        val argsSlot = slot<CodeGenArgs>()
        every { GRTClassFilesBuilderBase.builderFrom(capture(argsSlot)) } returns mockGRTBuilder

        SchemaObjectsBytecode().main(
            listOf("--generated_directory", generatedDir.absolutePath) +
                listOf("--schema_files", schemaFile.absolutePath) +
                listOf("--bytecode_worker_number", "0") +
                listOf("--bytecode_worker_count", "1") +
                listOf("--pkg_for_generated_classes_as_file", pkgFile.absolutePath)
        )

        verify { GRTClassFilesBuilderBase.builderFrom(any()) }
        with(argsSlot.captured) {
            assertEquals("com.test.package", pkgForGeneratedClasses)
        }
    }

    @Test
    fun `test with output archive`() {
        SchemaObjectsBytecode().main(
            listOf("--generated_directory", generatedDir.absolutePath) +
                listOf("--schema_files", schemaFile.absolutePath) +
                listOf("--bytecode_worker_number", "0") +
                listOf("--bytecode_worker_count", "1") +
                listOf("--output_archive", outputArchive.absolutePath)
        )

        verify { mockGRTBuilder.buildClassfiles(generatedDir) }
    }
}
