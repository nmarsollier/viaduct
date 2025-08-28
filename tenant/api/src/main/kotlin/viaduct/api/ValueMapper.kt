package viaduct.api

import graphql.schema.GraphQLObjectType
import viaduct.api.types.Object

/**
 * Provides a custom implementation for mapping a data type to a Viaduct object type.
 * Implementations should be registered using the service loader mechanism.
 */
@Suppress("unused")
interface ValueMapper<D : Any> {
    fun <V : Object> convert(
        from: D?,
        graphqlObjectType: GraphQLObjectType
    ): V?
}
