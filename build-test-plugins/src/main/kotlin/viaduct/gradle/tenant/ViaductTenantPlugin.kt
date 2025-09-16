package viaduct.gradle.tenant

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import viaduct.gradle.defaultschema.DefaultSchemaPlugin
import viaduct.gradle.utils.capitalize

/**
 * Plugin for generating Viaduct tenant code from GraphQL schemas.
 *
 * This version wires everything via Providers:
 *  - Generated sources are added to the existing 'main' source set (Java + Kotlin if present).
 *  - Schema project classpath is added via its 'main' SourceSet output (task-safe).
 */
abstract class ViaductTenantPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Ensure default schema resources exist (task-safe)
        DefaultSchemaPlugin.ensureApplied(project)

        // Create the DSL
        val extension = project.extensions.create<ViaductTenantExtension>("viaductTenant", project)

        // Java source sets (we always have Java plugin via Kotlin/Java ecosystem)
        val javaExt = project.extensions.getByType<JavaPluginExtension>()
        val mainJavaSS = javaExt.sourceSets.getByName("main")

        // Kotlin is optional; wire if present without hard dependency
        var addToKotlinMain: ((Any) -> Unit)? = null
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            val kotlinExt = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
            val mainKotlinSS = kotlinExt.sourceSets.getByName("main")
            addToKotlinMain = { provider -> mainKotlinSS.kotlin.srcDir(provider) }
        }

        // Lazily configure tenants after the DSL is set
        project.afterEvaluate {
            extension.tenantContainer.forEach { tenant ->
                val generateTask = configureTenant(project, tenant)

                // Wire GENERATOR OUTPUTS (sources) into existing 'main' source set.
                // These are DirectoryProperty providers from the task; adding them here
                // creates producer→consumer edges so compilation waits for generation.

                mainJavaSS.java.srcDir(generateTask.flatMap { it.resolverSrcDir })
                mainJavaSS.java.srcDir(generateTask.flatMap { it.modernModuleSrcDir })
                mainJavaSS.java.srcDir(generateTask.flatMap { it.metaInfSrcDir })

                addToKotlinMain?.invoke(generateTask.flatMap { it.resolverSrcDir })
                addToKotlinMain?.invoke(generateTask.flatMap { it.modernModuleSrcDir })
                // metaInf typically contains resources; if yours emits Kotlin/Java there too, keep it:
                addToKotlinMain?.invoke(generateTask.flatMap { it.metaInfSrcDir })

                // SCHEMA PROJECT CLASSPATH:
                // Instead of pointing at some build/generated directory, depend on the schema
                // project's 'main' SourceSet output — this pulls in its compiled classes/resources
                // and wires task dependencies correctly.
                val schemaProjectPath = tenant.schemaProjectPath.orNull
                    ?: throw GradleException("Schema Project Path is a required property")

                val schemaProject = project.project(schemaProjectPath)
                val schemaMainOutput = schemaProject
                    .extensions
                    .getByType(JavaPluginExtension::class.java)
                    .sourceSets
                    .getByName("main")
                    .output

                project.dependencies.add("implementation", schemaMainOutput)
            }
        }
    }

    /**
     * Registers the tenant generation task with lazy (provider-based) outputs.
     *
     * NOTE: ViaductTenantTask should annotate its output dirs with @OutputDirectory so that
     * these DirectoryPropertys act as proper task outputs.
     */
    private fun configureTenant(
        project: Project,
        tenant: ViaductTenant
    ): TaskProvider<ViaductTenantTask> {
        val name = tenant.name

        val packageName = tenant.packageName.orNull
            ?: throw GradleException("Package Name is a required property")

        val schemaProjectPath = tenant.schemaProjectPath.orNull
            ?: throw GradleException("Schema Project Path is a required property")

        // We only validate presence; actual classes come from schema project's 'main' output
        val schemaName = tenant.schemaName.orNull
            ?: throw GradleException("Schema Name is a required property")

        // Compute base generated root as a Provider (no mkdirs at configuration)
        val generatedRoot = project.layout.buildDirectory.dir("generated-sources")

        // Compute package path lazily
        val fullTenantPackage = "$packageName.$name"
        val packagePath = fullTenantPackage.replace('.', '/')

        // Output dirs (as DirectoryProperty providers)
        val resolverSrcDir = project.layout.buildDirectory.dir("generated-sources/$packagePath/resolverbases")
        val modernModuleSrcDir = project.layout.buildDirectory.dir("generated-sources/$packagePath/modernmodule")
        val metaInfSrcDir = project.layout.buildDirectory.dir("generated-sources/$packagePath/metainf")

        // Register generator task
        return project.tasks.register<ViaductTenantTask>("generate${name.capitalize()}Tenant") {
            group = "viaduct-tenant"
            description = "Generates Viaduct sources for tenant $name"

            // Inputs
            tenantName.set(name)
            packageNamePrefix.set(packageName)
            schemaFiles.from(tenant.schemaFiles)
            tenantFromSourceNameRegex.set(tenant.tenantFromSourceNameRegex)

            // Ensure default schema is available
            dependsOn("processResources")

            // Outputs (DirectoryProperty providers)
            this.resolverSrcDir.set(resolverSrcDir)
            this.modernModuleSrcDir.set(modernModuleSrcDir)
            this.metaInfSrcDir.set(metaInfSrcDir)
        }
    }
}
