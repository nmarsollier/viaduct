package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver
class ScalarStringResolver : QueryResolvers.ScalarString() {
    override suspend fun resolve(ctx: Context): String {
        return "tenant1 value"
    }
}
