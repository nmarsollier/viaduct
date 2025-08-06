package viaduct.gradle.schema

import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import viaduct.gradle.utils.capitalize

/**
 * This is a Build Src plugin to be used for creation of the Schema
 */
abstract class ViaductSchemaPlugin : Plugin<Project> {
    /**
     * @param project the implicit project that is going to be used
     */
    override fun apply(project: Project) {
        /**
         * This creates a viaductSchema DSL configuration to be used after the plugin is created
         * ex.
         *
         * viaductSchema {
         *     create("name") {
         *         // schema configuration
         *     }
         * }
         */
        val extension = project.extensions.create<ViaductSchemaExtension>("viaductSchema", project)

        /**
         * After the evaluation of the project (meaning the required details have been properly given),
         * each schema is configured in detail
         */
        project.afterEvaluate {
            if (extension.viaductAppMode.get()) {
                // Viaduct app mode: auto-discover and consolidate tenant schemas
                configureViaductAppSchema(project, extension)
            } else {
                // Standalone mode: configure explicitly created schemas
                extension.schemaContainer.forEach { schema ->
                    configureSchema(project, schema)
                }
            }
        }
    }

    /**
     * This function creates everything that is needed for each schema that is given, each in an isolated way.
     *
     * @param project the implicit project
     * @param schema The ViaductSchema details per each schema
     */
    private fun configureSchema(
        project: Project,
        schema: ViaductSchema
    ) {
        // Name will be given in the create("name") creation of the schema
        val name = schema.name

        // Required packageName that will be used to create the classes in
        val packageName = schema.grtPackageName.orNull
            ?: throw GradleException("Required property 'grtPackageName' not set for schema '$name'")

        // Configuration of the compile and Runtime classpath to recognize the generated Class dir
        val javaExtension = project.extensions.getByType<JavaPluginExtension>()

        val defaultSourceSets = setOf("main", "test")
        val effectiveSourceSets = schema.targetSourceSets.orNull.let {
            if (it.isNullOrEmpty()) {
                defaultSourceSets
            } else {
                it
            }
        }

        val taskName = "generate${name.capitalize()}Bytecode"
        val toolchainService = project.extensions.getByType(JavaToolchainService::class.java)
        val javaPluginExt = project.extensions.getByType(JavaPluginExtension::class.java)
        val javaExecutable = toolchainService
            .launcherFor(javaPluginExt.toolchain)
            .get()
            .executablePath
            .asFile
            .absolutePath

        // Check if we're in internal mode (tenant projects available) or standalone mode
        val mainProject = project.rootProject.findProject(":tenant:tenant-codegen")
        val apiProject = project.rootProject.findProject(":tenant:tenant-api")
        val isInternalMode = mainProject != null && apiProject != null

        val classpath = if (isInternalMode) {
            project.logger.info("Running in internal mode with tenant projects")
            /**
             * In order to get the Clikt Command we need to put the project where it is being used
             * as a classPath dependency so it can run.
             */
            val mainProject = project.project(":tenant:tenant-codegen")

            /**
             * The Clikt Command needs to have as runtime deps a list of Classes and Interfaces
             * that are present in tenant:api. Therefore, we must give them as classPath.
             */
            val apiProject = project.project(":tenant:tenant-api")

            effectiveSourceSets.forEach { sourceSetName ->
                javaExtension.sourceSets.named(sourceSetName) {
                    compileClasspath += project.files(schema.generatedClassesDir.get())
                    runtimeClasspath += project.files(schema.generatedClassesDir.get())

                    compileClasspath += apiProject.the<JavaPluginExtension>().sourceSets["main"].output
                    runtimeClasspath += apiProject.the<JavaPluginExtension>().sourceSets["main"].output

                    // Add the generated classes directory to the source sets (same as original)
                    java.srcDir(schema.generatedClassesDir.get())
                }
            }

            mainProject.the<JavaPluginExtension>().sourceSets["main"].runtimeClasspath +
                apiProject.the<JavaPluginExtension>().sourceSets["main"].runtimeClasspath
        } else {
            project.logger.info("Running in standalone mode without tenant projects")
            /**
             * If the project is not available, we need to add the classpath of the current project
             * as a dependency.
             *
             * This is the default case when the plugin is used in a project that is not part of the tenant module.
             */
            effectiveSourceSets.forEach { sourceSetName ->
                javaExtension.sourceSets.named(sourceSetName) {
                    // Add the generated classes directory to the source sets (same as original)
                    java.srcDir(schema.generatedClassesDir.get())
                }
            }

            /**
             * If plugin is being applied in non-oss env; it needs to be running in stand-alone mode, meaning it
             * should not depend on anything other than published Viaduct jar.
             *
             * If artifact resolution fails, as a last resort we assume required dependencies already exist in
             * the runtimeClasspath.
             */
            fallbackSchemaClassPath(project)
        }

        /**
         * Register the generation task where the schema will be created
         */
        val generateByteCodeTask = project.tasks.register<ViaductSchemaTask>(taskName) {
            group = "viaduct-schema"
            description = "Generates schema objects bytecode for $name"

            this.schemaName.set(name)
            this.packageName.set(packageName)
            this.schemaFiles.from(schema.schemaFiles)
            this.workerNumber.set(schema.workerNumber)
            this.workerCount.set(schema.workerCount)
            this.includeIneligibleForTesting.set(schema.includeIneligibleForTesting)
            this.mainProjectClasspath.from(classpath)
            this.javaExecutable.set(javaExecutable)
            this.generatedSrcDir.set(project.layout.buildDirectory.dir("generated-sources/schema/$name/generated_classes"))

            if (isInternalMode) {
                val mainProject = project.project(":tenant:tenant-codegen")
                dependsOn(mainProject.tasks.named("classes"))

                val apiProject = project.project(":tenant:tenant-api")
                dependsOn(apiProject.tasks.named("classes"))
            }
        }

        /**
         * Makes the compilation task for each source set dependent on schema generation.
         */
        effectiveSourceSets.forEach { sourceSet ->
            val srcSetCompileName = if (sourceSet == "main") {
                ""
            } else {
                sourceSet.capitalize()
            }

            val compileJavaTask = project.tasks.findByName("compile${srcSetCompileName}Java")
            compileJavaTask?.dependsOn(generateByteCodeTask)

            val compileKotlinTask = project.tasks.findByName("compile${srcSetCompileName}Kotlin")
            compileKotlinTask?.dependsOn(generateByteCodeTask)

            if (compileJavaTask == null && compileKotlinTask == null) {
                throw GradleException("No compile tasks found for source set '$sourceSet' in project '${project.name}'")
            }
        }

        val lintingTask = project.tasks.findByName("runKtlintCheckOverMainSourceSet")
        val lintTestFixtureTask = project.tasks.findByName("runKtlintCheckOverTestFixturesSourceSet")
        val lintingTestTask = project.tasks.findByName("runKtlintCheckOverTestSourceSet")
        lintingTask?.mustRunAfter(generateByteCodeTask)
        lintTestFixtureTask?.mustRunAfter(generateByteCodeTask)
        lintingTestTask?.mustRunAfter(generateByteCodeTask)
    }

    private fun viaductArtifactClassPath(project: Project): FileCollection {
        val dependency = project.dependencies.create("com.airbnb.viaduct:runtime:0.1.0-SNAPSHOT")
        val configuration = project.configurations.detachedConfiguration(dependency)
        return project.files(configuration.resolve())
    }

    private fun projectClassPath(project: Project): FileCollection = project.configurations.getByName("runtimeClasspath")

    /**
     * Attempts resolving published viaduct artifact so schema plugin works as stand-alone.
     * It should not depend on anything other than viaduct.jar.
     */
    private fun fallbackSchemaClassPath(project: Project): FileCollection =
        runCatching { viaductArtifactClassPath(project) }
            .getOrElse {
                project.logger.debug("Falling back to runtimeClasspath: ${it.message}")
                projectClassPath(project)
            }

    /**
     * Configures viaduct app mode: auto-discovers tenant schemas and creates combined schema
     */
    private fun configureViaductAppSchema(
        project: Project,
        extension: ViaductSchemaExtension
    ) {
        // Schema project discovers its parent's tenant directory
        val appProject = project.parent ?: throw GradleException("Schema project must have a parent for viaduct app mode")
        val tenantsDir = appProject.projectDir.resolve("tenants")

        if (!tenantsDir.exists()) {
            throw GradleException("Tenants directory not found: ${tenantsDir.absolutePath}")
        }

        // Auto-discover all tenant resource directories
        val tenantResourceDirs = getTenantResourceDirectories(tenantsDir)

        if (tenantResourceDirs.isEmpty()) {
            project.logger.warn("No tenant resource directories found in ${tenantsDir.absolutePath}")
            return
        }

        // Create single combined schema configuration on the schema project
        val combinedSchema = extension.create("combinedSchema") {
            grtPackageName.set("viaduct.api.grts")
            // Add each tenant resource directory as a schema source
            tenantResourceDirs.forEach { resourceDir ->
                schemaDirectory(resourceDir.absolutePath, "**/*.graphqls")
            }
        }

        // Configure the combined schema generation
        configureSchema(project, combinedSchema)

        // Create the JAR packaging task
        createCombinedSchemaJar(project, combinedSchema)

        // Add tenant-api dependency if available (for internal builds)
        // This makes TenantModule available to projects that depend on the schema project
        try {
            project.dependencies.add("api", project.project(":tenant:tenant-api"))
        } catch (_: Exception) {
            // Ignore for external builds - everything needed is in viaduct jar
        }
    }

    /**
     * Creates the combined schema JAR task
     */
    private fun createCombinedSchemaJar(
        project: Project,
        schema: ViaductSchema
    ) {
        val buildCombinedSchemaJar = project.tasks.register("buildCombinedSchemaJar", Jar::class.java) {
            group = "GraphQL Schema Codegen"
            description = "Combines all schema definitions into a single JAR, making the generated classes available at compile time."

            // Add explicit dependency on the schema generation task
            dependsOn("generate${schema.name.capitalize()}Bytecode")

            val finalJar = project.layout.buildDirectory.file("libs/final-generated-all.jar")
            val generatedDirFile = project.layout.buildDirectory.file("generated").get()

            doFirst {
                // Clean-up old jar
                finalJar.get().asFile.delete()
            }

            val outputJarFile = finalJar.get().asFile
            destinationDirectory.set(outputJarFile.parentFile)
            archiveFileName.set(outputJarFile.name)
            from(schema.generatedClassesDir)

            doLast {
                // Clean-up intermediates
                generatedDirFile.asFile.deleteRecursively()
            }
        }

        // Declare final-generated JAR as an API dependency so it's exposed transitively to consumers
        project.dependencies.add("api", project.files(buildCombinedSchemaJar.get().outputs).builtBy(buildCombinedSchemaJar))
    }

    /**
     * Discovers tenant resource directories containing GraphQL schemas
     */
    private fun getTenantResourceDirectories(tenantsDir: File): List<File> {
        return tenantsDir
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory }
            .map { File(it, "src/main/resources") }
            .filter { it.exists() && it.isDirectory }
    }
}
