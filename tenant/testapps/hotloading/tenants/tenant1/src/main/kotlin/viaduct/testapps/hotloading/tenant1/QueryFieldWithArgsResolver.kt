package viaduct.testapps.hotloading.tenant1

import viaduct.api.Resolver
import viaduct.testapps.hotloading.tenant1.resolverbases.QueryResolvers
import viaduct.testapps.hotloading.tenant1.resolverbases.QueryResolvers.FieldWithArgs.Context

@Resolver
class QueryFieldWithArgsResolver : QueryResolvers.FieldWithArgs() {
    override suspend fun resolve(ctx: Context): String? {
        return ctx.arguments.arg1
    }
}
