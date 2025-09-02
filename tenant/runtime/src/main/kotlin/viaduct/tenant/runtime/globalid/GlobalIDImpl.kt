package viaduct.tenant.runtime.globalid

import kotlin.reflect.full.isSubclassOf
import viaduct.api.globalid.GlobalID
import viaduct.api.reflect.Type
import viaduct.api.types.NodeCompositeOutput
import viaduct.api.types.NodeObject

data class GlobalIDImpl<T : NodeCompositeOutput>(
    override val type: Type<T>,
    override val internalID: String,
) : GlobalID<T> {
    init {
        require(type.kcls.isSubclassOf(NodeObject::class)) { "GlobalID type must be a NodeObject" }
    }
}
