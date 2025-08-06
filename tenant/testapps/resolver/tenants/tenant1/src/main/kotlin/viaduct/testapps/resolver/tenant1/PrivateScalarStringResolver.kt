package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver
class PrivateScalarStringResolver : QueryResolvers.PrivateScalarString() {
    override suspend fun resolve(ctx: Context): String {
        return "tenant1 private value"
    }
}
