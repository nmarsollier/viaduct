package viaduct.testapps.resolver.tenant2

import viaduct.api.Resolver
import viaduct.testapps.resolver.tenant2.resolverbases.QueryResolvers

@Resolver
class ObjectValue2Resolver : QueryResolvers.ObjectValue2() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Test2ObjectValue {
        return viaduct.api.grts.Test2ObjectValue.Builder(ctx).value("tenant2 value").build()
    }
}
