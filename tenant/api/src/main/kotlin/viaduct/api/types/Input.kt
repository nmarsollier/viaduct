package viaduct.api.types

import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLSchema

/**
 * Tagging interface for input types
 */
interface Input : InputLike {
    companion object {
        fun inputType(
            name: String,
            schema: GraphQLSchema
        ): GraphQLInputObjectType {
            return schema.getTypeAs(name)
        }
    }
}
