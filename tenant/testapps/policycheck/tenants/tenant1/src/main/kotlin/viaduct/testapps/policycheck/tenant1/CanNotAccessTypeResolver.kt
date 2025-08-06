package viaduct.testapps.policycheck.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.CanNotAccessPerson
import viaduct.testapps.policycheck.tenant1.resolverbases.QueryResolvers

@Resolver
class CanNotAccessTypeResolver : QueryResolvers.CanNotAccessType() {
    override suspend fun resolve(ctx: Context): CanNotAccessPerson {
        return CanNotAccessPerson.Builder(ctx).name("should not resolve").ssn("should not resolve").build()
    }
}
