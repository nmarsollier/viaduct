package viaduct.testapps.hotloading.tenant1

import viaduct.api.Resolver
import viaduct.testapps.hotloading.tenant1.resolverbases.QueryResolvers

@Resolver
class QueryField1Resolver : QueryResolvers.Field1() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Object1 {
        return viaduct.api.grts.Object1.Builder(ctx)
            .value1("tenant1 query field1")
            .fieldToBeAddedResolver("tenant1 query fieldToBeAddedResolver")
            .fieldAnotherModule("tenant1 query fieldAnotherModule")
            .build()
    }
}
