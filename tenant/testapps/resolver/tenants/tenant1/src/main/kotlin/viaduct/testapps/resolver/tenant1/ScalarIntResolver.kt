package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver
class ScalarIntResolver : QueryResolvers.ScalarInt() {
    override suspend fun resolve(ctx: Context): Int {
        return 123
    }
}
