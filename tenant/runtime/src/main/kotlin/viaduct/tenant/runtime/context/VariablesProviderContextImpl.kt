package viaduct.tenant.runtime.context

import viaduct.api.context.ExecutionContext
import viaduct.api.context.VariablesProviderContext
import viaduct.api.globalid.GlobalID
import viaduct.api.internal.InternalContext
import viaduct.api.internal.internal
import viaduct.api.reflect.Type
import viaduct.api.types.Arguments
import viaduct.api.types.Object

class VariablesProviderContextImpl<T : Arguments>(
    override val args: T,
    private val executionContext: ExecutionContext,
) : VariablesProviderContext<T>, ExecutionContext by executionContext, InternalContext by executionContext.internal {
    override fun <T : Object> globalIDFor(
        type: Type<T>,
        internalID: String
    ): GlobalID<T> = executionContext.globalIDFor(type, internalID)
}
