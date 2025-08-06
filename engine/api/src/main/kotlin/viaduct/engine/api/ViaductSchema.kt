package viaduct.engine.api

import graphql.schema.GraphQLSchema
import javax.inject.Inject
import viaduct.utils.graphql.GraphQLTypeRelations

data class ViaductSchema
    @Inject
    constructor(
        val schema: GraphQLSchema
    ) {
        val rels: GraphQLTypeRelations by lazy { GraphQLTypeRelations(schema) }
    }
