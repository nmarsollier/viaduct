package viaduct.gradle.defaultschema

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

/**
 * Plugin that automatically sets up default schema extraction for Viaduct projects.
 *
 * This plugin:
 * 1. Creates a task to extract default_schema.graphqls to a predictable build location
 * 2. Adds the output directory to test resources so tests can access it via classpath
 * 3. Provides a way for other plugins to depend on the default schema file
 */
abstract class DefaultSchemaPlugin : Plugin<Project> {
    companion object {
        const val TASK_NAME = "extractDefaultSchema"
        const val DEFAULT_SCHEMA_BUILD_PATH = "generated-resources/viaduct"
        const val DEFAULT_SCHEMA_FILENAME = "default_schema.graphqls"

        /**
         * Ensures the default schema plugin is applied to the project.
         * This allows other plugins to easily ensure the default schema is available.
         */
        fun ensureApplied(project: Project) {
            if (!project.plugins.hasPlugin(DefaultSchemaPlugin::class.java)) {
                project.pluginManager.apply(DefaultSchemaPlugin::class.java)
            }
        }
    }

    override fun apply(project: Project) {
        // Register the task to extract default schema
        val defaultSchemaTask = project.tasks.register<DefaultSchemaOutputTask>(TASK_NAME) {
            defaultSchemaFile.set(
                project.layout.buildDirectory.file("$DEFAULT_SCHEMA_BUILD_PATH/$DEFAULT_SCHEMA_FILENAME")
            )
        }

        // Add the output directory to test resources when java plugin is applied
        project.plugins.withId("java") {
            val javaExtension = project.extensions.getByType<JavaPluginExtension>()
            val testSourceSet = javaExtension.sourceSets.getByName("test")

            // Add the generated resources directory to test resources
            // This makes the default schema available to tests via classpath
            val generatedResourcesDir = project.layout.buildDirectory.dir(DEFAULT_SCHEMA_BUILD_PATH)
            testSourceSet.resources.srcDir(defaultSchemaTask.map { generatedResourcesDir })

            project.logger.info("Added default schema output directory to test resources")
        }

        // Integrate with common build lifecycle tasks
        project.tasks.named("processResources").configure {
            dependsOn(defaultSchemaTask)
        }
    }
}
