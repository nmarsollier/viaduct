package viaduct.gradle.classdiff

import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import viaduct.gradle.defaultschema.DefaultSchemaPlugin
import viaduct.gradle.utils.capitalize

/**
 * Plugin for generating schema and tenant code from configured schema diffs.
 * This plugin uses build.gradle configuration to define schema diffs with
 * their packages and schema resources.
 */
abstract class ViaductClassDiffPlugin : Plugin<Project> {
    companion object {
        private const val PLUGIN_GROUP = "viaduct-classdiff"
        private const val GENERATED_SOURCES_PATH = "generated-sources/classdiff"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create<ViaductClassDiffExtension>("viaductClassDiff", project)

        // Ensure default schema plugin is applied so default schema is available
        DefaultSchemaPlugin.ensureApplied(project)

        val javaExtension = project.extensions.getByType<JavaPluginExtension>()
        val testSourceSet = javaExtension.sourceSets.getByName("test")

        val generatedSourceSet = javaExtension.sourceSets.create("generated")
        generatedSourceSet.apply {
            val generatedSourcesDir = project.layout.buildDirectory.dir(GENERATED_SOURCES_PATH).get().asFile
            java.srcDir(generatedSourcesDir)

            compileClasspath += testSourceSet.compileClasspath
            runtimeClasspath += testSourceSet.runtimeClasspath
        }

        project.afterEvaluate {
            val schemaDiffs = extension.schemaDiffs.get()
            if (schemaDiffs.isEmpty()) {
                project.logger.info("No schema diffs configured")
                return@afterEvaluate
            }

            project.logger.info("Found ${schemaDiffs.size} configured schema diffs")

            val generationTasks = schemaDiffs.map { schemaDiff -> configureSchemaGenerationTasks(project, schemaDiff) }.flatten()
            generationTasks.forEach { task -> generatedSourceSet.java.srcDir(task.map { it.outputs.files }) }
        }
    }

    /**
     * Configure generation tasks for a specific schema diff
     */
    private fun configureSchemaGenerationTasks(
        project: Project,
        schemaDiff: SchemaDiff
    ): List<org.gradle.api.tasks.TaskProvider<*>> {
        val schemaFiles = schemaDiff.resolveSchemaFiles()

        if (schemaFiles.isEmpty()) {
            project.logger.error("No valid schema files found for schema diff '${schemaDiff.name}'")
            return emptyList()
        }

        val schemaTask = configureSchemaGeneration(project, schemaDiff, schemaFiles)
        val grtTask = configureGRTGeneration(project, schemaDiff, schemaFiles)

        // Ensure GRT task runs after schema task
        grtTask.configure {
            dependsOn(schemaTask)
        }

        return listOf(schemaTask, grtTask)
    }

    /**
     * Configure schema generation task for a schema diff
     */
    private fun configureSchemaGeneration(
        project: Project,
        schemaDiff: SchemaDiff,
        schemaFiles: List<File>
    ): org.gradle.api.tasks.TaskProvider<ViaductClassDiffSchemaTask> {
        return project.tasks.register<ViaductClassDiffSchemaTask>(
            "generateSchemaDiff${schemaDiff.name.capitalize()}SchemaObjects"
        ) {
            group = PLUGIN_GROUP
            description = "Generates schema objects for schema diff '${schemaDiff.name}'"
            schemaName.set("default")
            packageName.set(schemaDiff.actualPackage.get())
            workerNumber.set(0)
            workerCount.set(1)
            includeIneligibleForTesting.set(true)
            mainProjectClasspath.from(getCodegenProjectClassPath(project))
            this.schemaFiles.from(schemaFiles)
            this.javaExecutable.set(getJavaExecutable(project))
            generatedSrcDir.set(project.layout.buildDirectory.dir(GENERATED_SOURCES_PATH))

            // Depend on processResources to ensure default schema is available
            dependsOn("processResources")

            // Ensure output directory exists
            doFirst {
                generatedSrcDir.get().asFile.mkdirs()
            }
        }
    }

    /**
     * Configure GRT generation task for a schema diff
     */
    private fun configureGRTGeneration(
        project: Project,
        schemaDiff: SchemaDiff,
        schemaFiles: List<File>
    ): org.gradle.api.tasks.TaskProvider<ViaductClassDiffGRTKotlinTask> {
        val classpath = getCodegenProjectClassPath(project)
        val packageName = schemaDiff.expectedPackage.get()
        val packagePath = packageName.replace(".", "/")

        return project.tasks.register<ViaductClassDiffGRTKotlinTask>(
            "generateSchemaDiff${schemaDiff.name.capitalize()}KotlinGrts"
        ) {
            group = PLUGIN_GROUP
            description = "Generates Kotlin GRTs for schema diff '${schemaDiff.name}'"
            this.schemaFiles.from(schemaFiles)
            this.packageName.set(packageName)
            codegenClassPath.from(classpath)
            javaExecutable.set(getJavaExecutable(project))
            generatedSrcDir.set(project.layout.buildDirectory.dir("$GENERATED_SOURCES_PATH/$packagePath"))

            // Depend on processResources to ensure default schema is available
            dependsOn("processResources")

            // Ensure output directory exists
            doFirst {
                generatedSrcDir.get().asFile.mkdirs()
            }
        }
    }

    /**
     * Get Java executable for toolchain
     */
    private fun getJavaExecutable(project: Project): String {
        val toolchainService = project.extensions.getByType<JavaToolchainService>()
        val javaPluginExt = project.extensions.getByType<JavaPluginExtension>()

        return toolchainService
            .launcherFor(javaPluginExt.toolchain)
            .get()
            .executablePath
            .asFile
            .absolutePath
    }

    /**
     * Get the main project classpath for code generation
     */
    private fun getCodegenProjectClassPath(project: Project) =
        try {
            val codegenClassPath = project.findProject(":tenant:tenant-codegen")
            if (codegenClassPath != null) {
                val codegenSourceSet = codegenClassPath.the<JavaPluginExtension>().sourceSets["main"]
                codegenSourceSet.runtimeClasspath + codegenSourceSet.output + project.configurations.getByName("runtimeClasspath")
            } else {
                project.configurations.getByName("runtimeClasspath")
            }
        } catch (e: Exception) {
            project.logger.warn("Classpath resolution failed: ${e.message}")
            project.configurations.getByName("runtimeClasspath")
        }
}
