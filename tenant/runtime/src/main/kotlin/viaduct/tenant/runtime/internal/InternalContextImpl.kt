package viaduct.tenant.runtime.internal

import viaduct.api.globalid.GlobalIDCodec
import viaduct.api.internal.InternalContext
import viaduct.api.internal.ReflectionLoader
import viaduct.engine.api.ViaductSchema

class InternalContextImpl(
    override val schema: ViaductSchema,
    override val globalIDCodec: GlobalIDCodec,
    override val reflectionLoader: ReflectionLoader
) : InternalContext
