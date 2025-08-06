package viaduct.testapps.hotloading.tenant1

import viaduct.api.Resolver
import viaduct.testapps.hotloading.tenant1.resolverbases.QueryResolvers

@Resolver
class QueryFieldToBeUpdatedResolver : QueryResolvers.FieldToBeUpdated() {
    override suspend fun resolve(ctx: Context): String {
        return "tenant1 query fieldToBeUpdated"
    }
}
