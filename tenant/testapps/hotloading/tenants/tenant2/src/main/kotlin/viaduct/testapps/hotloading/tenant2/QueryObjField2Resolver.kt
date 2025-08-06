package viaduct.testapps.hotloading.tenant2

import viaduct.api.Resolver
import viaduct.api.grts.Object4
import viaduct.testapps.hotloading.tenant2.resolverbases.QueryResolvers

@Resolver
class QueryObjField2Resolver : QueryResolvers.ObjField2() {
    override suspend fun resolve(ctx: Context): Object4 {
        return Object4.Builder(ctx)
            .field1("tenant2 object4 field1")
            .build()
    }
}
