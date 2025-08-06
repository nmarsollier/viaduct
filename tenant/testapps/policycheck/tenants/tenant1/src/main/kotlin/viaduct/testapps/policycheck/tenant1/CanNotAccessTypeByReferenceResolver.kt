package viaduct.testapps.policycheck.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.CanNotAccessPerson
import viaduct.testapps.policycheck.tenant1.resolverbases.QueryResolvers

@Resolver
class CanNotAccessTypeByReferenceResolver : QueryResolvers.CanNotAccessTypeByReference() {
    override suspend fun resolve(ctx: Context): CanNotAccessPerson {
        return ctx.nodeFor(ctx.globalIDFor(CanNotAccessPerson.Reflection, ctx.arguments.id))
    }
}
