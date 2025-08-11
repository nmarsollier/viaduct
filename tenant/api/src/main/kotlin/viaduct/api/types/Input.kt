package viaduct.api.types

import graphql.schema.GraphQLInputObjectType
import viaduct.engine.api.ViaductSchema

/**
 * Tagging interface for input types
 */
interface Input : InputLike {
    companion object {
        fun inputType(
            name: String,
            schema: ViaductSchema
        ): GraphQLInputObjectType {
            return schema.schema.getTypeAs(name)
        }
    }
}
