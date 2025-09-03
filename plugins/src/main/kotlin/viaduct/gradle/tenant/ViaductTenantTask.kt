package viaduct.gradle.tenant

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
 * Task to generate tenant code for viaduct-tenant plugin.
 */
abstract class ViaductTenantTask : DefaultTask() {
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

    @TaskAction
    fun generateTenant() {
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

        // Build arguments for code generation (no --isFeatureAppTest flag for regular tenant plugin)
        val finalArgs = mutableListOf(
            "--tenant_pkg",
            tenantName.get(),
            "--schema_files",
            schemaFiles.files.joinToString(",") { it.absolutePath },
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
