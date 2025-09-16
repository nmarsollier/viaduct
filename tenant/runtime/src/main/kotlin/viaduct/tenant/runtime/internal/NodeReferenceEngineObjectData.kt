package viaduct.tenant.runtime.internal

import viaduct.engine.api.EngineObjectData
import viaduct.engine.api.NodeEngineObjectData
import viaduct.engine.api.UnsetSelectionException

/**
 * An EOD for Node references that wraps a NodeEngineObjectData. The purpose of this
 * class is to prevent tenants from attempting to access fields on a newly created
 * Node reference. This should not be passed to the engine.
 */
class NodeReferenceEngineObjectData(val nodeEngineObjectData: NodeEngineObjectData) : EngineObjectData {
    override val graphQLObjectType = nodeEngineObjectData.graphQLObjectType

    /**
     * @throws UnsetSelectionException for any [selection] other than id
     */
    override suspend fun fetch(selection: String): Any {
        if (selection == "id") return nodeEngineObjectData.id
        throw UnsetSelectionException(
            selection,
            graphQLObjectType,
            "only id can be accessed on an unresolved Node reference"
        )
    }
}
