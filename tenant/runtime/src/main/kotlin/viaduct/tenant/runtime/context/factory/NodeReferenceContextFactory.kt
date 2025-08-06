package viaduct.tenant.runtime.context.factory

import viaduct.api.internal.NodeReferenceFactory
import viaduct.engine.api.EngineExecutionContext
import viaduct.tenant.runtime.internal.NodeReferenceFactoryImpl

object NodeReferenceContextFactory {
    /** A default Factory<NodeReferenceFactory> suitable for any use */
    val default: Factory<EngineExecutionContext, NodeReferenceFactory> =
        Factory { engineExecutionContext ->
            NodeReferenceFactoryImpl { id, graphQLObjectType ->
                engineExecutionContext.createNodeEngineObjectData(
                    id,
                    graphQLObjectType,
                )
            }
        }
}
