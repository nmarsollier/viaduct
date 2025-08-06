package viaduct.engine.api

import graphql.schema.GraphQLObjectType

/**
 * Request-scoped execution context used to pass contextual elements to tenant API implementations
 */
interface EngineExecutionContext {
    val fullSchema: ViaductSchema
    val rawSelectionSetFactory: RawSelectionSet.Factory
    val rawSelectionsLoaderFactory: RawSelectionsLoader.Factory

    fun createNodeEngineObjectData(
        id: String,
        graphQLObjectType: GraphQLObjectType,
    ): NodeEngineObjectData

    // TODO(https://app.asana.com/1/150975571430/project/1203659453427089/task/1210861903745772?focus=true) - remove when everything has been shimmed
    fun hasModernNodeResolver(typeName: String): Boolean
}
