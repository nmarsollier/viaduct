package viaduct.testapps.hotloading.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.TestNode1
import viaduct.testapps.hotloading.tenant1.resolverbases.QueryResolvers

@Resolver
class QueryNodeReferenceResolver : QueryResolvers.NodeReference() {
    override suspend fun resolve(ctx: Context): TestNode1 {
        return ctx.nodeFor(ctx.globalIDFor(TestNode1.Reflection, ctx.arguments.id))
    }
}
