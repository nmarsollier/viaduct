package viaduct.gradle.tenant

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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
        // Ensure default schema resources exist
        DefaultSchemaPlugin.ensureApplied(project)

        // Create the DSL extension
        val extension = project.extensions.create<ViaductTenantExtension>("viaductTenant", project)

        // Store generation tasks for later dependency wiring
        val generationTasks = mutableListOf<TaskProvider<ViaductTenantTask>>()

        // Configure source sets early (not in afterEvaluate)
        val javaExt = project.extensions.getByType<JavaPluginExtension>()
        val mainJavaSS = javaExt.sourceSets.getByName("main")

        // Kotlin configuration if present
        var mainKotlinSS: org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet? = null
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            val kotlinExt = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
            mainKotlinSS = kotlinExt.sourceSets.getByName("main")
        }

        // Configure tenants after DSL is populated
        project.afterEvaluate {
            extension.tenantContainer.forEach { tenant ->
                val generateTask = configureTenant(project, tenant)
                generationTasks.add(generateTask)

                // Add generated sources to source sets using task outputs
                // This creates proper task dependencies via Gradle's provider chain
                mainJavaSS.java.srcDir(generateTask.flatMap { it.resolverSrcDir })
                mainJavaSS.java.srcDir(generateTask.flatMap { it.modernModuleSrcDir })
                mainJavaSS.resources.srcDir(generateTask.flatMap { it.metaInfSrcDir })

                // Add to Kotlin source set if present
                mainKotlinSS?.let { kotlinSS ->
                    kotlinSS.kotlin.srcDir(generateTask.flatMap { it.resolverSrcDir })
                    kotlinSS.kotlin.srcDir(generateTask.flatMap { it.modernModuleSrcDir })
                }

                // Schema project dependency
                val schemaProjectPath = tenant.schemaProjectPath.orNull
                    ?: throw GradleException("Schema Project Path is required")

                val schemaProject = project.project(schemaProjectPath)
                val schemaMainOutput = schemaProject
                    .extensions
                    .getByType(JavaPluginExtension::class.java)
                    .sourceSets
                    .getByName("main")
                    .output

                project.dependencies.add("implementation", schemaMainOutput)
            }

            // CRITICAL: Wire explicit task dependencies
            wireTaskDependencies(project, generationTasks)
        }
    }

    private fun wireTaskDependencies(
        project: Project,
        generationTasks: List<TaskProvider<ViaductTenantTask>>
    ) {
        // Make all compilation tasks depend on ALL generation tasks
        // This ensures generation completes before ANY compilation starts

        // Wire Java compilation
        project.tasks.named("compileJava") {
            generationTasks.forEach { genTask ->
                dependsOn(genTask)
            }
        }

        // Wire Kotlin compilation if present
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            // Use withType to catch all Kotlin compilation tasks
            project.tasks.withType<KotlinCompile>().configureEach {
                generationTasks.forEach { genTask ->
                    dependsOn(genTask)
                }
            }
        }

        // Also wire the classes task to ensure proper ordering
        project.tasks.named("classes") {
            generationTasks.forEach { genTask ->
                dependsOn(genTask)
            }
        }

        // For incremental builds, ensure source set outputs depend on generation
        val sourceSets = project.extensions.getByType<JavaPluginExtension>().sourceSets
        sourceSets.named("main") {
            output.dir(generationTasks.map { it.flatMap { task -> task.resolverSrcDir } })
            output.dir(generationTasks.map { it.flatMap { task -> task.modernModuleSrcDir } })
            output.dir(generationTasks.map { it.flatMap { task -> task.metaInfSrcDir } })
        }
    }

    private fun configureTenant(
        project: Project,
        tenant: ViaductTenant
    ): TaskProvider<ViaductTenantTask> {
        val name = tenant.name
        val packageName = tenant.packageName.orNull
            ?: throw GradleException("Package Name is required")
        val schemaProjectPath = tenant.schemaProjectPath.orNull
            ?: throw GradleException("Schema Project Path is required")

        // Compute paths
        val fullTenantPackage = "$packageName.$name"
        val packagePath = fullTenantPackage.replace('.', '/')

        // Register the generation task
        return project.tasks.register<ViaductTenantTask>("generate${name.capitalize()}Tenant") {
            group = "viaduct-tenant"
            description = "Generates Viaduct sources for tenant $name"

            // Inputs
            tenantName.set(name)
            packageNamePrefix.set(packageName)
            schemaFiles.from(tenant.schemaFiles)
            tenantFromSourceNameRegex.set(tenant.tenantFromSourceNameRegex)

            // Set output directories
            resolverSrcDir.set(
                project.layout.buildDirectory.dir("generated-sources/$packagePath/resolverbases")
            )
            modernModuleSrcDir.set(
                project.layout.buildDirectory.dir("generated-sources/$packagePath/modernmodule")
            )
            metaInfSrcDir.set(
                project.layout.buildDirectory.dir("generated-sources/$packagePath/metainf")
            )

            // Make generation depend on schema project compilation
            val schemaProject = project.project(schemaProjectPath)
            dependsOn(schemaProject.tasks.named("classes"))
        }
    }
}
