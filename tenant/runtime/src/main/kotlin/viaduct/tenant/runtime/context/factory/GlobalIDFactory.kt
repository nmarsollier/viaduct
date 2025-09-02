package viaduct.tenant.runtime.context.factory

import viaduct.api.globalid.GlobalID
import viaduct.api.internal.InternalContext
import viaduct.api.types.NodeCompositeOutput

class GlobalIDArgs(
    val internalContext: InternalContext,
    /** a request-scoped un-decoded GlobalID value */
    val globalID: String,
)

object GlobalIDFactory {
    /** A default `Factory<GlobalIDArgs, GlobalID>` suitable for general use */
    val default: Factory<GlobalIDArgs, GlobalID<*>> =
        Factory { args ->
            args.internalContext.globalIDCodec.deserialize<NodeCompositeOutput>(
                args.globalID
            )
        }
}
