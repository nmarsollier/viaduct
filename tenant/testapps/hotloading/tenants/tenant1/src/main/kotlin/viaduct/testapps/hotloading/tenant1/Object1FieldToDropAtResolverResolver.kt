package viaduct.testapps.hotloading.tenant1

import viaduct.api.Resolver
import viaduct.testapps.hotloading.tenant1.resolverbases.Object1Resolvers

@Resolver
class Object1FieldToDropAtResolverResolver : Object1Resolvers.FieldToDropAtResolver() {
    override suspend fun resolve(ctx: Context): String {
        return "tenant1 object1 fieldToDropAtResolver"
    }
}
