package viaduct.gradle.schema

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.Property

/**
 * Viaduct Schema entry point. this is the gradle extensions, where multiple schema can be created.
 * This configures them in kotlin dsl language with the details of each schema.
 * viaductSchema {
 *   create("schema_name") {
 *       packageName.set("schema_package_name")
 *       schemaDirectory("/src/main/resources")
 *   }
 * }
 */
open class ViaductSchemaExtension(private val project: Project) {
    /**
     * The container to hold all schemas directly
     */
    val schemaContainer: NamedDomainObjectContainer<ViaductSchema> =
        project.container(ViaductSchema::class.java) { name ->
            ViaductSchema(name, project)
        }

    /**
     * When true, enables viaduct app mode where the plugin automatically discovers
     * and consolidates schemas from parent project's tenants directory
     */
    val viaductAppMode: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    /**
     * Allows direct creation of a named schema with configuration
     */
    fun create(
        name: String,
        configuration: Action<ViaductSchema>
    ): ViaductSchema {
        val schema = schemaContainer.create(name)
        configuration.execute(schema)
        return schema
    }
}
