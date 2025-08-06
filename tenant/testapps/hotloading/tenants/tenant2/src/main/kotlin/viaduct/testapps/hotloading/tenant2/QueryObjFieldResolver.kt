package viaduct.testapps.hotloading.tenant2

import viaduct.api.Resolver
import viaduct.api.grts.Object3
import viaduct.testapps.hotloading.tenant2.resolverbases.QueryResolvers

@Resolver
class QueryObjFieldResolver : QueryResolvers.ObjField() {
    override suspend fun resolve(ctx: Context): Object3 {
        return Object3.Builder(ctx)
            .field1("tenant2 object3 field1")
            .field3("tenant2 object3 field3")
            .build()
    }
}
