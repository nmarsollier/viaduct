package viaduct.testapps.hotloading.tenant2

import viaduct.api.Resolver
import viaduct.api.grts.Object5
import viaduct.api.grts.SomeEnum
import viaduct.testapps.hotloading.tenant2.resolverbases.QueryResolvers

@Resolver
class QueryObjField3Resolver : QueryResolvers.ObjField3() {
    override suspend fun resolve(ctx: Context): Object5 {
        return Object5.Builder(ctx)
            .enumField(SomeEnum.VALUE1)
            .build()
    }
}
