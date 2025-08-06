package viaduct.gradle.tenant

import org.gradle.api.tasks.TaskAction
import viaduct.gradle.common.ViaductTenantTaskBase

/**
 * Task to generate tenant code for viaduct-schema plugin (main source set).
 * This extends the common base class but is separate from ViaductFeatureAppTenantTask
 * to avoid conflicts when both viaduct-schema and viaduct-feature-app plugins
 * are used in the same build.
 */
abstract class ViaductTenantTask : ViaductTenantTaskBase() {
    override val featureAppTest: Boolean = false

    @TaskAction
    fun generateTenant() {
        executeTenantGeneration()
    }
}
