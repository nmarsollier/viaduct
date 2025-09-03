package viaduct.gradle.tenant

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property

/**
 * This class represents a single tenant configuration that will be configured by name.
 * @param name the Name which the tasks will be created and the location of the generated classes will be based on.
 * @param project the implicit Gradle Project.
 */
open class ViaductTenant(val name: String, private val project: Project) {
    /**
     * This property holds the packageName where the tenant will be created
     */
    val packageName: Property<String> = project.objects.property(String::class.java)

    /**
     * The Schema files to be configured for generation
     */
    val schemaFiles: ConfigurableFileCollection = project.files()

    /**
     * the project path where schema is configured
     */
    val schemaProjectPath: Property<String> = project.objects.property(String::class.java)

    /**
     * the name used by the schema which is created
     */
    val schemaName: Property<String> = project.objects.property(String::class.java)

    /**
     * Regex pattern to extract a tenant name from a full source file path
     */
    val tenantFromSourceNameRegex: Property<String> = project.objects.property(String::class.java)
        .convention("tenants/(.*)/src/main")

    /**
     * Helper function to get the Schema resources
     */
    fun schemaDirectory(
        dir: String,
        pattern: String = "*.graphqls"
    ) {
        val fileTree = project.fileTree(dir) {
            include(pattern)
        }
        schemaFiles.from(fileTree)
    }
}
