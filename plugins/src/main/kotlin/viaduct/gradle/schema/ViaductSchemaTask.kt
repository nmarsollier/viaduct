package viaduct.gradle.schema

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

/**
 * Task to generate schema objects for viaduct-schema plugin.
 */
abstract class ViaductSchemaTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val schemaName: Property<String>

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val workerNumber: Property<Int>

    @get:Input
    abstract val workerCount: Property<Int>

    @get:Input
    abstract val includeIneligibleForTesting: Property<Boolean>

    @get:Input
    abstract val javaExecutable: Property<String>

    @get:InputFiles
    abstract val schemaFiles: ConfigurableFileCollection

    @get:InputFiles
    abstract val mainProjectClasspath: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val generatedSrcDir: DirectoryProperty

    @TaskAction
    fun generateSchema() {
        val outputDir = generatedSrcDir.get().asFile
        val classpath = mainProjectClasspath.asPath

        val schemaFilesArg = schemaFiles.files.joinToString(",") { it.absolutePath }
        val workerNumberArg = workerNumber.get().toString()
        val workerCountArg = workerCount.get().toString()
        val includeEligibleForTesting = includeIneligibleForTesting.get()

        // Clean and prepare directories
        if (outputDir.exists()) outputDir.deleteRecursively()
        outputDir.mkdirs()

        val baseArgs = mutableListOf(
            "--generated_directory",
            outputDir.absolutePath,
            "--schema_files",
            schemaFilesArg,
            "--bytecode_worker_number",
            workerNumberArg,
            "--bytecode_worker_count",
            workerCountArg,
            "--pkg_for_generated_classes",
            packageName.get()
        )

        val finalArgs = if (includeEligibleForTesting) {
            baseArgs + "--include_ineligible_for_testing_only"
        } else {
            baseArgs
        }

        val result = execOperations.exec {
            executable = javaExecutable.get()
            args = listOf(
                "-cp",
                classpath,
                "viaduct.tenant.codegen.cli.SchemaObjectsBytecode\$Main"
            ) + finalArgs
            isIgnoreExitValue = true
        }

        if (result.exitValue != 0) {
            throw GradleException("SchemaObjectsBytecode execution failed with exit code ${result.exitValue}")
        }

        // Ensure the generated directory has content
        if (!outputDir.exists() || (outputDir.listFiles()?.isEmpty() != false)) {
            throw GradleException("Schema generation failed - no classes generated in ${outputDir.absolutePath}")
        }
    }
}
