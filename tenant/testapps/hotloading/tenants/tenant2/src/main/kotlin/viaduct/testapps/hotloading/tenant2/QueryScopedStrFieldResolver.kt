package viaduct.testapps.hotloading.tenant2

import viaduct.api.Resolver
import viaduct.testapps.hotloading.tenant2.resolverbases.QueryResolvers

@Resolver
class QueryScopedStrFieldResolver : QueryResolvers.ScopedStrField() {
    override suspend fun resolve(ctx: Context): String {
        return "tenant2 query scopedStrField"
    }
}
