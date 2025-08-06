package viaduct.testapps.policycheck.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.CanAccessPerson
import viaduct.testapps.policycheck.tenant1.resolverbases.QueryResolvers

@Resolver
class CanAccessTypeByReferenceResolver : QueryResolvers.CanAccessTypeByReference() {
    override suspend fun resolve(ctx: Context): CanAccessPerson {
        return ctx.nodeFor(ctx.globalIDFor(CanAccessPerson.Reflection, ctx.arguments.id))
    }
}
