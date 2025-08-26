package viaduct.gradle.app

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import viaduct.gradle.schema.ViaductSchemaExtension
import viaduct.gradle.tenant.ViaductTenantExtension
import viaduct.gradle.utils.computeChildProjectPath

class ViaductAppPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        checkTenantsDirectoryExists(project)

        val schemaProject = checkSchemaProjectExists(project)
        checkSchemaProjectDoesNotContainBuildLogic(schemaProject)

        // Configure [ViaductAppExtension]
        val appExt = project.extensions.create("viaduct", ViaductAppExtension::class.java)
        appExt.appProjectProperty.set(project)

        // Configure schema project for viaduct app mode
        schemaProject.pluginManager.apply("java-library")
        schemaProject.pluginManager.apply("viaduct-schema")

        schemaProject.extensions.configure(JavaPluginExtension::class.java) {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }

        // Enable viaduct app mode for automatic schema discovery
        schemaProject.extensions.configure(ViaductSchemaExtension::class.java) {
            viaductAppMode.set(true)
        }

        // Set-up conventions for tenant plugin for all subprojects. They can overwrite if they want.
        project.subprojects.forEach { sub ->
            sub.pluginManager.withPlugin("viaduct-tenant") {
                val tenantPluginExt = sub.extensions.getByType(ViaductTenantExtension::class.java)
                tenantPluginExt.tenantContainer.configureEach {
                    schemaProjectPath.convention(schemaProject.path)
                }
            }
        }

        // declare dynamic dependencies, setting up schema dependency for any subproject applying tenant plugin.
        project.gradle.projectsEvaluated {
            project.subprojects
                .filter { it.plugins.hasPlugin("viaduct-tenant") }
                .forEach { sub ->
                    // Add the dependency to point to the dynamic schema project
                    sub.dependencies.add("implementation", project.project(schemaProject.path))
                }
        }

        validateTenantIntegrity(project)
    }

    /**
     * Validates the integrity of all tenant projects within the given project.
     * Ensures that tenants follow architectural constraints by checking:
     * - No tenant contains nested tenant subprojects
     * - No tenant has dependencies on other tenants
     *
     * Validation occurs after all projects are evaluated to ensure complete
     * dependency graphs are available for analysis.
     */
    private fun validateTenantIntegrity(project: Project) {
        project.gradle.projectsEvaluated {
            val tenantProjects = project.subprojects.filter {
                it.pluginManager.hasPlugin("viaduct-tenant")
            }

            // Validate no nested tenants
            tenantProjects.forEach { it.validateNestedSubProjects() }

            // Validate no tenant-to-tenant dependencies
            validateTenantDependencies(tenantProjects)
        }
    }

    /**
     * Validates that tenant projects do not have dependencies on each other.
     * Checks api and implementation configurations for cross-tenant dependencies
     * and throws an exception if any are found.
     */
    private fun validateTenantDependencies(tenantProjects: List<Project>) {
        val tenantNames = tenantProjects.mapTo(mutableSetOf()) { it.name }
        val errors = mutableListOf<String>()

        tenantProjects.forEach { tenant ->
            val dependencies = tenant.configurations
                .filter { it.name in setOf("api", "implementation") }
                .flatMap { it.allDependencies }

            val tenantDependencies = dependencies.filter { it.name in tenantNames }
            if (tenantDependencies.isNotEmpty()) {
                val dependencyNames = tenantDependencies.joinToString(", ") { it.name }
                errors.add("Tenant \"${tenant.projectDir}\" has dependencies on other tenants: $dependencyNames")
            }
        }

        if (errors.isNotEmpty()) {
            throw GradleException("Tenant dependency violations found:\n${errors.joinToString("\n")}")
        }
    }

    /**
     * Validates that the given tenant project does not contain any nested tenants.
     * Recursively checks all subprojects for the viaduct-tenant plugin and throws
     * an exception if any are found.
     */
    private fun Project.validateNestedSubProjects() {
        if (this.subprojects.isEmpty()) {
            return
        } else {
            this.subprojects.forEach {
                if (it.pluginManager.hasPlugin("viaduct-tenant")) {
                    throw GradleException("Tenant: \"${this.projectDir}\" contains a tenant nested within.")
                }
                it.validateNestedSubProjects()
            }
        }
    }
}

private fun checkTenantsDirectoryExists(project: Project) {
    val tenantsDir = project.projectDir.resolve("tenants")

    if (!tenantsDir.exists() || !tenantsDir.isDirectory) {
        throw GradleException("Expected a 'tenants' directory directly under ${project.projectDir}")
    }
}

private fun checkSchemaProjectExists(project: Project): Project {
    val dynamicSchemaProjectPath = computeChildProjectPath(project, "schema")
    val schemaProject = project.rootProject.findProject(dynamicSchemaProjectPath)
        ?: throw GradleException("Expected schema project at path '$dynamicSchemaProjectPath' (sibling to ${project.path})")
    return schemaProject
}

private fun checkSchemaProjectDoesNotContainBuildLogic(schemaProject: Project) {
    val schemaBuildFile = schemaProject.projectDir.resolve("build.gradle.kts")
    if (schemaBuildFile.exists()) {
        val invalidLines = schemaBuildFile.readLines()
            .map { it.trim() }
            .filter { line ->
                line.isNotEmpty() &&
                    !line.startsWith("//") &&
                    !line.startsWith("/*") &&
                    !line.startsWith("*")
            }

        if (invalidLines.isNotEmpty()) {
            throw GradleException(
                "Project ':schema' must not contain custom configuration in build.gradle.kts. Found:\n" +
                    invalidLines.joinToString("\n")
            )
        }
    }
}
