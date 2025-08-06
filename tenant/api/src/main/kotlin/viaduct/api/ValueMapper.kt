package viaduct.api

import graphql.schema.GraphQLObjectType
import viaduct.api.types.Object

@Suppress("unused")
interface ValueMapper<D : Any> {
    fun <V : Object> convert(
        from: D?,
        graphqlObjectType: GraphQLObjectType
    ): V?
}
