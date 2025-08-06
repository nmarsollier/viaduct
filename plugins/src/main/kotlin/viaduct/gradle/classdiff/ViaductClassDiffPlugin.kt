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
import viaduct.gradle.utils.capitalize
import viaduct.gradle.utils.configureKotlinSourceSet

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

        // Eagerly create the generated sources directory
        val generatedSourcesDir = project.layout.buildDirectory.dir(GENERATED_SOURCES_PATH).get().asFile
        generatedSourcesDir.mkdirs()

        project.afterEvaluate {
            val schemaDiffs = extension.schemaDiffs.get()
            if (schemaDiffs.isEmpty()) {
                project.logger.info("No schema diffs configured")
                return@afterEvaluate
            }

            project.logger.info("Found ${schemaDiffs.size} configured schema diffs")

            val generationTasks = schemaDiffs.map { schemaDiff ->
                configureSchemaGenerationTasks(project, schemaDiff)
            }.flatten()

            configureSourceSets(project)
            linkGenerationTasksToCompilation(project, generationTasks)
        }
    }

    /**
     * Configure source sets to include generated directories after generation tasks are created
     */
    private fun configureSourceSets(project: Project) {
        val javaExtension = project.extensions.getByType<JavaPluginExtension>()
        val generatedSourcesDir = project.layout.buildDirectory.dir(GENERATED_SOURCES_PATH).get().asFile

        // Ensure directory exists
        generatedSourcesDir.mkdirs()

        // Configure Java test source set
        javaExtension.sourceSets.getByName("test").apply {
            java.srcDir(generatedSourcesDir)
            compileClasspath += project.files(generatedSourcesDir)
            runtimeClasspath += project.files(generatedSourcesDir)
        }

        configureKotlinSourceSet(project, generatedSourcesDir)
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

    /**
     * Link generation tasks to compilation tasks to ensure proper build order
     */
    private fun linkGenerationTasksToCompilation(
        project: Project,
        generationTasks: List<org.gradle.api.tasks.TaskProvider<*>>
    ) {
        if (generationTasks.isEmpty()) return

        // Create a single task that depends on all generation tasks
        val allGenerationTask = project.tasks.register("generateAllSchemaDiffSources") {
            group = PLUGIN_GROUP
            description = "Generates all schema diff sources"
            dependsOn(generationTasks)
        }

        // Make compilation tasks depend on the umbrella task
        val compilationTaskNames = listOf(
            "compileTestJava",
            "compileTestKotlin",
            "processTestResources",
            "testClasses"
        )

        compilationTaskNames.forEach { taskName ->
            project.tasks.findByName(taskName)?.dependsOn(allGenerationTask)
        }

        // Make sure test task also depends on generation
        project.tasks.findByName("test")?.dependsOn(allGenerationTask)

        // Ensure linting runs after generation
        val lintingTasks = listOf(
            "runKtlintCheckOverMainSourceSet",
            "runKtlintCheckOverTestSourceSet",
            "runKtlintCheckOverTestFixturesSourceSet"
        )

        lintingTasks.forEach { taskName ->
            project.tasks.findByName(taskName)?.mustRunAfter(allGenerationTask)
        }

        project.logger.info("Linked ${generationTasks.size} generation tasks to compilation via umbrella task")
    }
}
