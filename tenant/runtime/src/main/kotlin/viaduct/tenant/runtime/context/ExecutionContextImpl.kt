package viaduct.tenant.runtime.context

import viaduct.api.context.ExecutionContext
import viaduct.api.internal.InternalContext
import viaduct.api.reflect.Type
import viaduct.api.types.Object
import viaduct.tenant.runtime.globalid.GlobalIDImpl

/**
 * Implementation for ExecutionContext, used to delegate common implementations for context subclasses.
 */
open class ExecutionContextImpl(
    private val internal: InternalContext
) : ExecutionContext, InternalContext by internal {
    override fun <T : Object> globalIDFor(
        type: Type<T>,
        internalID: String
    ) = GlobalIDImpl(type, internalID)
}
