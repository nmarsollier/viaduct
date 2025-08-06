package viaduct.testapps.hotloading.tenant1

import viaduct.api.Resolver
import viaduct.testapps.hotloading.tenant1.resolverbases.QueryResolvers
import viaduct.testapps.hotloading.tenant1.resolverbases.QueryResolvers.FieldWithInput2.Context

@Resolver
class QueryFieldWithInput2Resolver : QueryResolvers.FieldWithInput2() {
    override suspend fun resolve(ctx: Context): String {
        return ctx.arguments.input.strField
    }
}
