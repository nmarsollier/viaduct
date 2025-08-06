package viaduct.testapps.hotloading.tenant2

import viaduct.api.Resolver
import viaduct.testapps.hotloading.tenant2.resolverbases.Object4Resolvers

@Resolver
class Object4Field2Resolver : Object4Resolvers.Field2() {
    override suspend fun resolve(ctx: Context): String {
        return "tenant2 object4 field2"
    }
}
