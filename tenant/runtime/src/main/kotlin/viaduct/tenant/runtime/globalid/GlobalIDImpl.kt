package viaduct.tenant.runtime.globalid

import kotlin.reflect.full.isSubclassOf
import viaduct.api.globalid.GlobalID
import viaduct.api.reflect.Type
import viaduct.api.types.CompositeOutput
import viaduct.api.types.Object

data class GlobalIDImpl<T : CompositeOutput>(
    override val type: Type<T>,
    override val internalID: String,
) : GlobalID<T> {
    init {
        require(type.kcls.isSubclassOf(Object::class)) { "GlobalID type must be a concrete object" }
    }
}
