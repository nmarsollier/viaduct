package viaduct.engine.api

import graphql.schema.GraphQLObjectType

class UnsetSelectionException(private val selection: String, private val objectType: GraphQLObjectType, private val details: String? = null) : Exception() {
    private val isField = objectType.getField(selection) != null
    override val message: String
        get() {
            val extra = details?.let { ": $it" }
            return if (isField) {
                "Attempted to access field ${objectType.name}.$selection but it was not set$extra"
            } else {
                "Attempted to access aliased field $selection but it was not set$extra"
            }
        }
}
