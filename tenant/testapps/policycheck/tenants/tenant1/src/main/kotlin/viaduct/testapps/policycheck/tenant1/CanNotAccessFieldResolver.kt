package viaduct.testapps.policycheck.tenant1

import viaduct.api.Resolver
import viaduct.testapps.policycheck.tenant1.resolverbases.QueryResolvers

@Resolver
class CanNotAccessFieldResolver : QueryResolvers.CanNotAccessField() {
    override suspend fun resolve(ctx: Context): String {
        return "should not resolve"
    }
}
