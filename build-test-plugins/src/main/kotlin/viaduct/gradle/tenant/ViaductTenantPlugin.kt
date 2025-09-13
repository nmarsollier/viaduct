package viaduct.gradle.tenant

import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import viaduct.gradle.defaultschema.DefaultSchemaPlugin
import viaduct.gradle.utils.capitalize
import viaduct.gradle.viaduct

/**
 * Plugin for generating Viaduct tenant code from GraphQL schemas
 */
abstract class ViaductTenantPlugin : Plugin<Project> {
    /**
     * @param project the implicit project that is going to be used
     */
    override fun apply(project: Project) {
        // Ensure default schema plugin is applied so default schema is available
        DefaultSchemaPlugin.ensureApplied(project)

        /**
         * This creates a viaductTenant DSL configuration to be used after the plugin is created
         * ex.
         *
         * viaductTenant {
         *     schemaProjectPath = ":schema" // New configuration
         *     create("name") {
         *         // schema configuration
         *     }
         * }
         */
        val extension = project.extensions.create<ViaductTenantExtension>("viaductTenant", project)

        // Create a special source set directory for generated code
        val generatedSourcesDir = project.layout.buildDirectory.dir("generated-sources").get().asFile
        generatedSourcesDir.mkdirs()

        // Kotlin Compatibility - adds the generatedSourcesDir as a srcDir for kotlin if kotlin exists
        // This allows the tenant in kotlin to recognize the new created directory code.
        val kotlinExtension = project.extensions.findByName("kotlin")
        if (kotlinExtension != null) {
            val sourceSetContainer = kotlinExtension.javaClass.getMethod("getSourceSets").invoke(kotlinExtension)
            val mainSourceSet = sourceSetContainer.javaClass.getMethod("getByName", String::class.java)
                .invoke(sourceSetContainer, "main")

            val kotlinSrcDir = mainSourceSet.javaClass.getMethod("getKotlin").invoke(mainSourceSet)
            kotlinSrcDir.javaClass.getMethod("srcDir", Any::class.java).invoke(kotlinSrcDir, generatedSourcesDir)
        }

        /**
         * After the evaluation of the project (meaning the required details have been properly given),
         * each tenant is configured in detail
         */
        project.afterEvaluate { // TODO: replace with lazy configuration, but for that lazy wiring of task properties is required first
            val javaExtension = project.extensions.getByType<JavaPluginExtension>()
            extension.tenantContainer.forEach { tenant ->
                val task = configureTenant(project, tenant, generatedSourcesDir)
                javaExtension.sourceSets.getByName("main").apply {
                    java.srcDir(task.map { it.outputs.files })
                }
            }
        }
    }

    /**
     * This function configures each tenant with its generatedSourceDir. and runs the clikt command
     * for each tenant and then each created file is then pulled into the directory where it can be
     * visible from the tenant
     *
     * @param project the implicit project
     * @param tenant the Configured tenant
     * @param generatedSourcesDir The Generate Source Dir
     */
    private fun configureTenant(
        project: Project,
        tenant: ViaductTenant,
        generatedSourcesDir: File
    ): TaskProvider<ViaductTenantTask> {
        // The tenant given name through the parameter create(name)
        val name = tenant.name

        // Tenant PackageName is required where the build classes will be copied to
        val packageName = tenant.packageName.orNull ?: throw GradleException(
            "Package Name is required property"
        )

        // Check for schema project path and schema name
        val schemaProjectPath = tenant.schemaProjectPath.orNull ?: throw GradleException(
            "Schema Project Path is required property"
        )

        val schemaName = tenant.schemaName.orNull ?: throw GradleException(
            "Schema Name is required property"
        )

        // Get reference to the schema project
        val schemaProject = project.project(schemaProjectPath)

        // Calculate schema generated classes directory path
        val schemaGeneratedClassesDir = schemaProject.layout.buildDirectory.dir("generated-sources/schema/$schemaName/generated_classes").get().asFile

        // Add schema generated classes directory to the compilation classpath via a file dependency
        project.dependencies.add("implementation", project.files(schemaGeneratedClassesDir))

        val archivesDir = project.layout.buildDirectory.dir("archives/tenant").get().asFile
        archivesDir.mkdirs()

        // Determine package paths up-front during configuration
        val fullTenantPackage = "$packageName.$name"
        val packagePath = fullTenantPackage.replace('.', File.separatorChar)
        val packageDir = File(generatedSourcesDir, packagePath)

        // Create the necessary directories
        packageDir.mkdirs()

        // Target directories for extracted files
        val resolverSrcDir = project.layout.buildDirectory.dir("generated-sources/$packagePath/resolverbases").get().asFile
        val modernModuleSrcDir = project.layout.buildDirectory.dir("generated-sources/$packagePath/modernmodule").get().asFile
        val metaInfSrcDir = project.layout.buildDirectory.dir("generated-sources/$packagePath/metainf").get().asFile

        // Main project for dependencies
        var mainProject = project.viaduct.appProject.get()
        val tenantCodegenProject = project.rootProject.findProject(":tenant:tenant-codegen")
        val mainProjectInternalClasspath: FileCollection? = if (tenantCodegenProject != null) {
            project.logger.info("Found tenant-codegen project, using internal classpath")
            // We add the libraries in the classpath, this is the tenant-codegen project for internal tests
            mainProject = project.project(":tenant:tenant-codegen")
            mainProject.the<JavaPluginExtension>().sourceSets["main"].runtimeClasspath
        } else {
            project.logger.info("No tenant-codegen project found, using standalone mode")
            // The external projects cannot access to classpath tenant:tenant-codegen.
            // This path is the normal scenario when the final user is using the app outside the project.
            // Below in the code, the library that will be used is the viaduct runtime dependency.
            null
        }

        val toolchainService = project.extensions.getByType(JavaToolchainService::class.java)
        val javaPluginExt = project.extensions.getByType(JavaPluginExtension::class.java)
        val javaExecutable = toolchainService
            .launcherFor(javaPluginExt.toolchain)
            .get()
            .executablePath
            .asFile
            .absolutePath

        // Get schema generation task from schema project if available

        // Register the generation task where the tenant will be created
        val generateTask = project.tasks.register<ViaductTenantTask>("generate${name.capitalize()}Tenant") {
            group = "viaduct-tenant"
            description = "Generates Viaduct sources for tenant $name"

            // Input properties
            this.tenantName.set(name)
            this.packageNamePrefix.set(packageName)
            this.schemaFiles.from(tenant.schemaFiles)
            this.tenantFromSourceNameRegex.set(tenant.tenantFromSourceNameRegex)

            // Using the plugin internally we use the main project classpath, witch is tenant-codegen
            mainProjectInternalClasspath?.let {
                this.mainProjectClasspath.from(it)
                dependsOn(mainProject.tasks.named("classes"))
            } ?: run {
                // When we use the plugin from standalone apps, we rely on the maven runtime dependency
                // Look for the runtime dependency in the tenant project's configuration instead of the apex project
                project
                    .configurations
                    .getByName("runtimeClasspath")
                    .resolvedConfiguration
                    .resolvedArtifacts
                    .find { artifact ->
                        artifact.moduleVersion.id.group == "com.airbnb.viaduct" &&
                            artifact.moduleVersion.id.name == "runtime"
                    }?.moduleVersion?.id?.version
                    ?.let { tenantCodegenVersion ->
                        this.mainProjectClasspath.from(
                            project.configurations.detachedConfiguration(
                                project.dependencies.create("com.airbnb.viaduct:runtime:$tenantCodegenVersion")
                            )
                        )
                    }
                    ?: run {
                        throw GradleException(
                            "Could not resolve com.airbnb.viaduct:runtime dependency, this dependency should be included in the tenant project."
                        )
                    }
            }
            this.javaExecutable.set(javaExecutable)

            // Depend on processResources to ensure default schema is available
            dependsOn("processResources")

            // Temporary directories
            this.modernModuleSrcDir.set(modernModuleSrcDir)
            this.resolverSrcDir.set(resolverSrcDir)
            this.metaInfSrcDir.set(metaInfSrcDir)
        }

        return generateTask
    }
}
