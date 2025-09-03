package viaduct.gradle.schema

import org.gradle.api.tasks.TaskAction
import viaduct.gradle.common.ViaductSchemaTaskBase

/**
 * Task to generate schema objects for viaduct-schema plugin (main source set).
 * This extends the common base class but is separate from ViaductFeatureAppSchemaTask
 * to avoid conflicts when both viaduct-schema and viaduct-feature-app plugins
 * are used in the same build.
 */
abstract class ViaductSchemaTask : ViaductSchemaTaskBase() {
    @TaskAction
    fun generateSchema() {
        executeSchemaGeneration()
    }
}
