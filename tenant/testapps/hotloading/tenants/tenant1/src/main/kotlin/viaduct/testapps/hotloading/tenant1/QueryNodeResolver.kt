package viaduct.testapps.hotloading.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.TestNode1
import viaduct.testapps.hotloading.tenant1.resolverbases.QueryResolvers

@Resolver
class QueryNodeResolver : QueryResolvers.Node() {
    override suspend fun resolve(ctx: Context): TestNode1 {
        return TestNode1.Builder(ctx).id(ctx.globalIDFor(TestNode1.Reflection, ctx.arguments.id)).key("testNodeKey").build()
    }
}
