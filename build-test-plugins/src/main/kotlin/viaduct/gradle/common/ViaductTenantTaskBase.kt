package viaduct.gradle.common

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.process.ExecOperations

/**
 * Base class for tenant generation tasks.
 * Contains common functionality shared between viaduct-schema and viaduct-feature-app plugins.
 */
abstract class ViaductTenantTaskBase : DefaultTask() {
    @get:Input
    abstract val featureAppTest: Boolean

    @get:Input
    abstract val tenantName: Property<String>

    @get:Input
    abstract val packageNamePrefix: Property<String>

    @get:Input
    abstract val javaExecutable: Property<String>

    @get:Input
    abstract val tenantFromSourceNameRegex: Property<String>

    @get:InputFiles
    abstract val schemaFiles: ConfigurableFileCollection

    @get:InputFiles
    abstract val mainProjectClasspath: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val modernModuleSrcDir: DirectoryProperty

    @get:OutputDirectory
    abstract val resolverSrcDir: DirectoryProperty

    @get:OutputDirectory
    abstract val metaInfSrcDir: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Inject
    abstract val projectLayout: ProjectLayout

    /**
     * Common tenant generation logic that can be called by subclasses
     */
    protected fun executeTenantGeneration() {
        val classpath = mainProjectClasspath.asPath

        // Get temporary generation directories
        val modernModuleSrcDirFile = modernModuleSrcDir.get().asFile
        val resolverSrcDirFile = resolverSrcDir.get().asFile
        val metaInfSrcDirFile = metaInfSrcDir.get().asFile

        // Ensure directories exist
        modernModuleSrcDirFile.mkdirs()
        resolverSrcDirFile.mkdirs()
        metaInfSrcDirFile.mkdirs()

        // Skip if no schema files
        if (schemaFiles.isEmpty) {
            return
        }

        // Include the default schema along with the configured schema files
        val allSchemaFiles = DefaultSchemaUtil.getSchemaFilesIncludingDefault(schemaFiles, projectLayout, logger)

        // Build arguments for code generation
        val baseArgs = mutableListOf(
            "--tenant_pkg",
            tenantName.get(),
            "--schema_files",
            allSchemaFiles.joinToString(",") { it.absolutePath },
            "--modern_module_generated_directory",
            modernModuleSrcDirFile.absolutePath,
            "--resolver_generated_directory",
            resolverSrcDirFile.absolutePath,
            "--metainf_generated_directory",
            metaInfSrcDirFile.absolutePath,
            "--tenant_package_prefix",
            packageNamePrefix.get(),
            "--tenant_from_source_name_regex",
            tenantFromSourceNameRegex.get()
        )

        val finalArgs = if (featureAppTest) {
            baseArgs + "--isFeatureAppTest"
        } else {
            baseArgs
        }

        // Execute code generation
        val result = execOperations.exec {
            executable = javaExecutable.get()
            args = listOf(
                "-cp",
                classpath,
                "viaduct.tenant.codegen.cli.ViaductGenerator\$Main"
            ) + finalArgs
            isIgnoreExitValue = true
        }

        if (result.exitValue != 0) {
            throw GradleException("ViaductGenerator execution failed with exit code ${result.exitValue}")
        }
    }
}
