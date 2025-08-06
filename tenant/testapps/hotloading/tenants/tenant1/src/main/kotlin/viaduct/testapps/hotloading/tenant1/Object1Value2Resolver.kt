package viaduct.testapps.hotloading.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.Object2
import viaduct.api.grts.TestEnum
import viaduct.testapps.hotloading.tenant1.resolverbases.Object1Resolvers

@Resolver
class Object1Value2Resolver : Object1Resolvers.Value2() {
    override suspend fun resolve(ctx: Context): Object2 {
        return Object2.Builder(ctx)
            .strField("tenant1 object2 strField")
            .enumField(TestEnum.VALUE2)
            .build()
    }
}
