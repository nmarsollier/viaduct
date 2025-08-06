package viaduct.gradle.feature

import java.io.File
import org.gradle.api.GradleException
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
 * Plugin for automatically discovering FeatureApp files and generating
 * both schema and tenant code for each discovered file using existing tasks
 */
abstract class ViaductFeatureAppPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<ViaductFeatureAppExtension>("viaductFeatureApp", project)

        project.afterEvaluate {
            val featureAppFiles = discoverFeatureAppFiles(project, extension)
            if (featureAppFiles.isEmpty()) {
                return@afterEvaluate
            }
            featureAppFiles.forEach { featureAppFile ->
                configureFeatureApp(project, featureAppFile, extension)
            }

            configureSourceSetsAfterTasks(project)
        }
    }

    /**
     * Configure source sets to include generated directories after tasks are created
     * Note: Only configures test source set since viaduct-feature-app is for testing only
     */
    private fun configureSourceSetsAfterTasks(project: Project) {
        val javaExtension = project.extensions.getByType<JavaPluginExtension>()
        val generatedSourcesDir = project.layout.buildDirectory.dir("generated-sources/featureapp").get().asFile

        javaExtension.sourceSets.getByName("test").apply {
            compileClasspath += project.files(generatedSourcesDir)
            runtimeClasspath += project.files(generatedSourcesDir)
        }

        configureKotlinSourceSet(project, generatedSourcesDir)
    }

    /**
     * Discover FeatureApp files in the project
     */
    private fun discoverFeatureAppFiles(
        project: Project,
        extension: ViaductFeatureAppExtension
    ): List<File> {
        val featureAppFiles = mutableListOf<File>()
        val pattern = extension.fileNamePattern.get().toRegex()
        val files = project.projectDir.walkTopDown()
            .filter { it.isFile }
            .filter { it.extension in listOf("kt", "java") }
            .filter { pattern.containsMatchIn(it.name) }
            .toList()
        files.forEach { file ->
            if (isFeatureAppFile(file)) {
                featureAppFiles.add(file)
            }
        }

        return featureAppFiles.distinctBy { it.absolutePath }
    }

    /**
     * Check if a file is a FeatureApp by examining its content
     */
    private fun isFeatureAppFile(file: File): Boolean {
        return try {
            val content = file.readText()

            // Skip base classes and abstract classes
            if (content.contains("interface FeatureAppTestBase") ||
                file.name == "FeatureAppTestBase.kt"
            ) {
                return false
            }

            // Must have either schema markers or override sdl
            val hasSchemaMarker = content.contains("#START_SCHEMA") && content.contains("#END_SCHEMA")
            val hasOverrideSdl = content.contains(Regex("override\\s+var\\s+sdl\\s*="))

            hasSchemaMarker || hasOverrideSdl
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Configure schema and tenant generation for a specific FeatureApp file
     */
    private fun configureFeatureApp(
        project: Project,
        featureAppFile: File,
        extension: ViaductFeatureAppExtension
    ) {
        // Extract a clean name for the FeatureApp
        val fileName = featureAppFile.nameWithoutExtension
        val featureAppName = fileName
            .replace("FeatureAppTest", "")
            .replace("FeatureApp", "")
            .replace("Test", "")
            .lowercase()
            .ifEmpty { "default" }

        val packageName = extractPackageFromFile(featureAppFile) ?: "${extension.basePackageName.get()}.$featureAppName"
        if (!packageName.contains(".")) {
            throw GradleException("Invalid package name '$packageName'. Package name must contain at least one segment (e.g., 'com.example.feature')")
        }

        @Suppress("DEPRECATION")
        val schemaDir = File(project.buildDir, "featureapp-schemas")
        val schemaFile = File(schemaDir, "$featureAppName.graphql")
        schemaDir.mkdirs()

        try {
            extractSchemaFromFeatureApp(featureAppFile, schemaFile)
        } catch (e: Exception) {
            project.logger.error("Failed to extract schema from ${featureAppFile.name}: ${e.message}")
            return
        }

        val schemaTask = configureSchemaGeneration(project, featureAppName, schemaFile, packageName)
        val tenantTask = configureTenantGeneration(project, featureAppName, schemaFile, packageName, schemaTask)

        // Link tasks to compilation
        linkToCompilationTasks(project, schemaTask)
        linkToCompilationTasks(project, tenantTask)
    }

    /**
     * Extract package name from Kotlin/Java file
     */
    private fun extractPackageFromFile(file: File): String? {
        return try {
            val content = file.readText()
            val packagePattern = Regex("^\\s*package\\s+([\\w.]+)", RegexOption.MULTILINE)
            val match = packagePattern.find(content)
            match?.groupValues?.get(1)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extract GraphQL schema from FeatureApp file
     */
    private fun extractSchemaFromFeatureApp(
        featureAppFile: File,
        outputFile: File
    ) {
        val content = featureAppFile.readText()
        var schemaContent: String? = null

        // Try to find schema between #START_SCHEMA and #END_SCHEMA markers
        val schemaMarkerPattern = Regex(
            """#START_SCHEMA\s*\n(.*?)\n\s*#END_SCHEMA""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        )

        val markerMatch = schemaMarkerPattern.find(content)
        if (markerMatch != null) {
            val rawSchema = markerMatch.groupValues[1]
            schemaContent = cleanupSchema(rawSchema)
        }

        // If no markers found, try to extract from sdl property
        if (schemaContent == null) {
            val sdlPattern = Regex(
                """override\s+var\s+sdl\s*=\s*"{3}(.*?)"{3}""",
                setOf(RegexOption.DOT_MATCHES_ALL)
            )

            val sdlMatch = sdlPattern.find(content)
            if (sdlMatch != null) {
                val rawSchema = sdlMatch.groupValues[1]
                schemaContent = cleanupSchema(rawSchema)
            }
        }

        if (schemaContent.isNullOrBlank()) {
            throw GradleException("No valid GraphQL schema found in ${featureAppFile.name}")
        }

        outputFile.parentFile.mkdirs()
        outputFile.writeText(schemaContent)
    }

    /**
     * Clean up extracted schema content
     */
    private fun cleanupSchema(rawSchema: String): String {
        return rawSchema.lines()
            .map { line ->
                line.trimStart()
                    .removePrefix("|")
                    .trimStart()
            }
            .filter { line ->
                line.isNotBlank() &&
                    !line.startsWith("#START_SCHEMA") &&
                    !line.startsWith("#END_SCHEMA")
            }
            .joinToString("\n")
            .trim()
    }

    /**
     * Configure schema generation using ViaductSchemaTask
     */
    private fun configureSchemaGeneration(
        project: Project,
        featureAppName: String,
        schemaFile: File,
        packageName: String,
    ): org.gradle.api.tasks.TaskProvider<ViaductFeatureAppSchemaTask> {
        val toolchainService = project.extensions.getByType<JavaToolchainService>()
        val javaPluginExt = project.extensions.getByType<JavaPluginExtension>()
        val javaExecutable = toolchainService
            .launcherFor(javaPluginExt.toolchain)
            .get()
            .executablePath
            .asFile
            .absolutePath

        val classpath = getMainProjectClasspath(project)

        return project.tasks.register<ViaductFeatureAppSchemaTask>(
            "generate${featureAppName.capitalize()}SchemaObjects"
        ) {
            group = "viaduct-feature-app"
            description = "Generates schema objects for FeatureApp $featureAppName"

            this.schemaName.set("default")
            this.packageName.set(packageName)
            this.workerNumber.set(0)
            this.workerCount.set(1)
            this.includeIneligibleForTesting.set(true)
            this.schemaFiles.from(schemaFile)
            this.mainProjectClasspath.from(classpath)
            this.javaExecutable.set(javaExecutable)
            this.generatedSrcDir.set(project.layout.buildDirectory.dir("generated-sources/featureapp/schema/$featureAppName"))
        }
    }

    /**
     * Configure tenant generation using ViaductTenantTask
     */
    private fun configureTenantGeneration(
        project: Project,
        featureAppName: String,
        schemaFile: File,
        packageName: String,
        schemaTask: org.gradle.api.tasks.TaskProvider<ViaductFeatureAppSchemaTask>?
    ): org.gradle.api.tasks.TaskProvider<ViaductFeatureAppTenantTask> {
        val tenantName = packageName.split(".").last()
        val tenantPackageName = packageName.split(".").dropLast(1).joinToString(".")

        val schemaOutputDir = project.layout.buildDirectory.dir("generated-sources/featureapp/schema/$featureAppName").get()

        // Add schema generated classes directory to the test classpath only (not implementation)
        // This prevents the generated sources from leaking to consuming projects
        project.dependencies.add("testImplementation", project.files(schemaOutputDir))

        val toolchainService = project.extensions.getByType<JavaToolchainService>()
        val javaPluginExt = project.extensions.getByType<JavaPluginExtension>()
        val javaExecutable = toolchainService
            .launcherFor(javaPluginExt.toolchain)
            .get()
            .executablePath
            .asFile
            .absolutePath

        val baseClasspath = getMainProjectClasspath(project)
        val extendedClasspath = baseClasspath + project.files(schemaOutputDir)
        return project.tasks.register<ViaductFeatureAppTenantTask>(
            "generate${featureAppName.capitalize()}Tenant"
        ) {
            group = "viaduct-feature-app"
            description = "Generates tenant code for FeatureApp $featureAppName"

            this.tenantName.set(tenantName)
            this.packageNamePrefix.set(tenantPackageName)
            this.schemaFiles.from(schemaFile)
            this.tenantFromSourceNameRegex.set(".*")
            this.mainProjectClasspath.from(extendedClasspath)
            this.javaExecutable.set(javaExecutable)
            this.modernModuleSrcDir.set(project.layout.buildDirectory.dir("generated-sources/featureapp/tenant/$featureAppName/modernmodule"))
            this.resolverSrcDir.set(project.layout.buildDirectory.dir("generated-sources/featureapp/tenant/$featureAppName/resolverbases"))
            this.metaInfSrcDir.set(project.layout.buildDirectory.dir("generated-sources/featureapp/tenant/$featureAppName/META-INF"))

            // Depend on schema generation if both are enabled
            schemaTask?.let { dependsOn(it) }
        }
    }

    /**
     * Get the main project classpath for code generation
     */
    private fun getMainProjectClasspath(project: Project) =
        try {
            // Try to get from specific projects first
            val mainProject = project.findProject(":tenant:tenant-codegen")
            val apiProject = project.findProject(":tenant:tenant-api")

            if (mainProject != null && apiProject != null) {
                mainProject.the<JavaPluginExtension>().sourceSets["main"].runtimeClasspath +
                    apiProject.the<JavaPluginExtension>().sourceSets["main"].runtimeClasspath
            } else {
                // Fallback to current project's runtime classpath
                project.configurations.getByName("runtimeClasspath")
            }
        } catch (e: Exception) {
            project.logger.info("Using fallback classpath: ${e.message}")
            project.configurations.getByName("runtimeClasspath")
        }

    /**
     * Link generation tasks to test compilation tasks with proper dependencies
     * Note: Only links to test tasks since viaduct-feature-app is for testing only
     */
    private fun linkToCompilationTasks(
        project: Project,
        generationTask: org.gradle.api.tasks.TaskProvider<*>
    ) {
        // Make test compilation tasks depend on generation (not main compilation)
        listOf("compileTestJava", "compileTestKotlin").forEach { taskName ->
            project.tasks.findByName(taskName)?.dependsOn(generationTask)
        }

        // Make processTestResources depend on generation tasks to handle META-INF files for tests
        project.tasks.findByName("processTestResources")?.dependsOn(generationTask)

        // Make linting tasks run after generation
        listOf(
            "runKtlintCheckOverMainSourceSet",
            "runKtlintCheckOverTestSourceSet",
            "runKtlintCheckOverTestFixturesSourceSet"
        ).forEach { taskName ->
            project.tasks.findByName(taskName)?.mustRunAfter(generationTask)
        }
    }
}
