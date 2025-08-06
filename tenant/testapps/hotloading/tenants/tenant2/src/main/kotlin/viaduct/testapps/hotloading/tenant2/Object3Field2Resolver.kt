package viaduct.testapps.hotloading.tenant2

import viaduct.api.Resolver
import viaduct.testapps.hotloading.tenant2.resolverbases.Object3Resolvers

@Resolver
class Object3Field2Resolver : Object3Resolvers.Field2() {
    override suspend fun resolve(ctx: Context): String {
        return "tenant2 object3 field2"
    }
}
