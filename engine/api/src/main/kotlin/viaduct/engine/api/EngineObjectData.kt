package viaduct.engine.api

import graphql.schema.GraphQLObjectType

/**
 * This interface can be interpreted as an untyped representation of GRT
 */
interface EngineObjectData {
    /**
     * Fetch a value that was selected with the provided [selection]
     *
     * @param selection a field or alias name
     * @throws UnsetSelectionException if the selection is unset
     */
    suspend fun fetch(selection: String): Any?

    val graphQLObjectType: GraphQLObjectType
}
