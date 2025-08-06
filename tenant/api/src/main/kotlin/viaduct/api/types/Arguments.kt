package viaduct.api.types

import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLSchema
import viaduct.utils.string.decapitalize

/**
 * Tagging interface for virtual input types that wrap field arguments
 */
interface Arguments : InputLike {
    /** A marker object indicating the lack of schematic arguments */
    object NoArguments : Arguments

    companion object {
        fun inputType(
            name: String,
            schema: GraphQLSchema
        ): GraphQLInputObjectType {
            val typeName = name.split("_").firstOrNull() ?: throw IllegalArgumentException("Invalid Arguments class name: $name")
            val fieldName = name.split("_").getOrNull(1)?.decapitalize() ?: throw IllegalArgumentException("Invalid Arguments class name: $name")
            val field = schema.getObjectType(typeName)?.getField(fieldName) ?: throw IllegalArgumentException("Field $typeName.$fieldName not found")
            val fields = field.arguments.map {
                val builder = GraphQLInputObjectField.Builder()
                    .name(it.name)
                    .type(it.type)
                if (it.hasSetDefaultValue() && it.argumentDefaultValue.isLiteral) {
                    val v = it.argumentDefaultValue.value as graphql.language.Value<*>
                    builder.defaultValueLiteral(v)
                }
                builder.build()
            }
            if (fields.isEmpty()) throw IllegalArgumentException("No arguments found for field $typeName.$fieldName")
            return GraphQLInputObjectType.Builder()
                .name(name)
                .fields(fields)
                .build()
        }
    }
}
