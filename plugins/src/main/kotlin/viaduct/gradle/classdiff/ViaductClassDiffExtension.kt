package viaduct.gradle.classdiff

import java.io.File
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Extension for configuring ClassDiff code generation.
 *
 * This extension allows configuration of multiple schema diffs, each with their own
 * packages and schema resources. The plugin will generate code for each configured
 * schema diff independently.
 */
open class ViaductClassDiffExtension(private val project: Project) {
    /**
     * List of schema diff configurations.
     * Each schema diff can have its own packages and schema resources.
     */
    val schemaDiffs: ListProperty<SchemaDiff> = project.objects.listProperty(SchemaDiff::class.java)

    /**
     * Create and configure a new schema diff.
     */
    fun schemaDiff(
        name: String,
        configure: SchemaDiff.() -> Unit
    ) {
        val schemaDiff = SchemaDiff(name, project)
        schemaDiff.configure()
        schemaDiffs.add(schemaDiff)
    }
}

/**
 * Configuration for a single schema diff, including packages and schema resources.
 */
class SchemaDiff(
    val name: String,
    private val project: Project
) {
    /**
     * Package name for generated schema objects (actuals).
     * Default: "actuals.api.generated.{name}"
     */
    val actualPackage: Property<String> = project.objects.property(String::class.java)
        .convention("actuals.api.generated.$name")

    /**
     * Package name for generated GRT objects (expected).
     * Default: "viaduct.api.grts.{name}"
     */
    val expectedPackage: Property<String> = project.objects.property(String::class.java)
        .convention("viaduct.api.grts.$name")

    /**
     * List of schema resource file paths.
     * These can be relative to the project root or absolute paths.
     */
    private val schemaResources: ListProperty<String> = project.objects.listProperty(String::class.java)

    /**
     * Add a schema resource file path.
     */
    fun schemaResource(path: String) {
        schemaResources.add(path)
    }

    /**
     * Add multiple schema resource file paths.
     */
    @Suppress("unused")
    fun schemaResources(vararg paths: String) {
        schemaResources.addAll(paths.toList())
    }

    /**
     * Resolve schema resource files to actual File objects.
     */
    internal fun resolveSchemaFiles(): List<File> {
        return schemaResources.get().mapNotNull { resourcePath ->
            findResourceFile(resourcePath)
        }
    }

    /**
     * Find resource file in common locations.
     */
    private fun findResourceFile(resourcePath: String): File? {
        val commonResourcePaths = listOf(
            // Direct path from project root
            resourcePath,
            // Standard resource directories
            "src/main/resources/$resourcePath",
            "src/test/resources/$resourcePath",
            // Relative to src directory
            "src/$resourcePath",
            // In schema-specific directories
            "schemas/$resourcePath",
            "src/main/resources/schemas/$resourcePath",
            "src/test/resources/schemas/$resourcePath"
        )

        val resolvedFile = commonResourcePaths
            .map { File(project.projectDir, it) }
            .firstOrNull { it.exists() && it.isFile }

        if (resolvedFile == null) {
            project.logger.warn("Schema resource not found: $resourcePath")
        }

        return resolvedFile
    }
}
