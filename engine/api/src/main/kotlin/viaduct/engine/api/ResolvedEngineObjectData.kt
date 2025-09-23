package viaduct.engine.api

import graphql.schema.GraphQLObjectType

class ResolvedEngineObjectData private constructor(
    override val graphQLObjectType: GraphQLObjectType,
    val data: Map<String, Any?>
) : EngineObjectData, Map<String, Any?> by data {
    override suspend fun fetch(selection: String): Any? {
        return fetchSync(selection)
    }

    /**
     * A non-suspend [fetch] that can be called from other non-suspend functions
     */
    fun fetchSync(selection: String): Any? {
        if (!data.containsKey(selection)) {
            throw UnsetSelectionException(
                selection,
                graphQLObjectType,
                "Please set a value for $selection using the builder for ${graphQLObjectType.name}"
            )
        }
        return data[selection]
    }

    // internal for testing
    internal fun selectionsThatAreSet(): Set<String> = data.keys

    class Builder(override val graphQLObjectType: GraphQLObjectType) : EngineObjectDataBuilder {
        private val data = mutableMapOf<String, Any?>()

        override fun put(
            selection: String,
            value: Any?
        ): Builder {
            data[selection] = value
            return this
        }

        override fun build() = ResolvedEngineObjectData(graphQLObjectType, data)
    }
}
