package viaduct.tenant.runtime.execution

import viaduct.api.internal.ObjectBase
import viaduct.engine.api.EngineObjectData
import viaduct.tenant.runtime.internal.NodeReferenceEngineObjectData

/**
 * Unwraps the object GRT into an EngineObjectData to provide to the engine.
 * For node references, this will extract the wrapped NodeEngineObjectData.
 */
fun ObjectBase.unwrap(): EngineObjectData {
    return when (val eod = this.engineObjectData) {
        is NodeReferenceEngineObjectData -> eod.nodeEngineObjectData
        else -> eod
    }
}
