package viaduct.service.runtime

import viaduct.engine.api.ViaductSchema

/**
 * Immutable configuration for creating ViaductSchemaRegistry instances.
 * This is the output of SchemaRegistryConfiguration and input to ViaductSchemaRegistry.Factory.
 */
data class SchemaRegistryConfigurationInfo(
    val graphQLSchemaFactory: GraphQLSchemaFactory,
    val publicSchemaRegistration: List<PublicSchemaRegistration>,
    val asyncSchemaRegistration: Map<String, SchemaRegistryConfiguration.AsyncScopedSchema>,
    val fullSchema: ViaductSchema?
) {
    /**
     * Creates a new configuration with the specified full schema.
     * Used when the schema is provided explicitly.
     */
    fun withFullSchema(schema: ViaductSchema): SchemaRegistryConfigurationInfo {
        return copy(fullSchema = schema)
    }
}
