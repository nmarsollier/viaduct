package viaduct.gradle.schema

import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import viaduct.gradle.defaultschema.DefaultSchemaPlugin
import viaduct.gradle.utils.capitalize

/**
 * This is a Build Src plugin to be used for creation of the Schema
 */
abstract class ViaductSchemaPlugin : Plugin<Project> {
    /**
     * @param project the implicit project that is going to be used
     */
    override fun apply(project: Project) {
        // Ensure default schema plugin is applied so default schema is available
        DefaultSchemaPlugin.ensureApplied(project)

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

        val taskName = "generate${name.capitalize()}Bytecode"

        // Check if we're in internal mode (tenant projects available) or standalone mode
        val mainProject = project.rootProject.findProject(":tenant:tenant-codegen")
        val apiProject = project.rootProject.findProject(":tenant:tenant-api")
        val isInternalMode = mainProject != null && apiProject != null

        /**
         * Register the generation task where the schema will be created
         */
        project.tasks.register<ViaductSchemaTask>(taskName) {
            group = "viaduct-schema"
            description = "Generates schema objects bytecode for $name"

            this.schemaName.set(name)
            this.packageName.set(packageName)
            this.schemaFiles.from(schema.schemaFiles)
            this.workerNumber.set(schema.workerNumber)
            this.workerCount.set(schema.workerCount)
            this.includeIneligibleForTesting.set(schema.includeIneligibleForTesting)
            this.generatedSrcDir.set(project.layout.buildDirectory.dir("generated-sources/schema/$name/generated_classes"))

            // Depend on processResources to ensure default schema is available
            dependsOn("processResources")

            if (isInternalMode) {
                dependsOn(mainProject!!.tasks.named("classes"))

                dependsOn(apiProject!!.tasks.named("classes"))
            }
        }
    }

    private fun viaductArtifactClassPath(project: Project): FileCollection {
        val dependency = project.dependencies.create("com.airbnb.viaduct:runtime:0.1.0")
        val configuration = project.configurations.detachedConfiguration(dependency)
        return project.files(configuration.resolve())
    }

    private fun projectClassPath(project: Project): FileCollection = project.configurations.getByName("runtimeClasspath")

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
