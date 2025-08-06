package viaduct.testapps.hotloading.tenant1

import viaduct.api.Resolver
import viaduct.testapps.hotloading.tenant1.resolverbases.QueryResolvers
import viaduct.testapps.hotloading.tenant1.resolverbases.QueryResolvers.FieldWithInput.Context

@Resolver
class QueryFieldWithInputResolver : QueryResolvers.FieldWithInput() {
    override suspend fun resolve(ctx: Context): String {
        return "tenant1 fieldWithInput " + ctx.arguments.input.strField
    }
}
