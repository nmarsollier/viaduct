package viaduct.testapps.policycheck.tenant1

import viaduct.api.Resolver
import viaduct.testapps.policycheck.tenant1.resolverbases.QueryResolvers

@Resolver
class CanAccessFieldResolver : QueryResolvers.CanAccessField() {
    override suspend fun resolve(ctx: Context): String {
        return "can see field"
    }
}
