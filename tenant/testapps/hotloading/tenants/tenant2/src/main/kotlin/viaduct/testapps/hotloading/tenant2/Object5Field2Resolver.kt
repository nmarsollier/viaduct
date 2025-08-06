package viaduct.testapps.hotloading.tenant2

import viaduct.api.Resolver
import viaduct.api.grts.SomeEnum
import viaduct.testapps.hotloading.tenant2.resolverbases.Object5Resolvers

@Resolver
class Object5Field2Resolver : Object5Resolvers.Field2() {
    override suspend fun resolve(ctx: Context): String {
        val enumValue = ctx.arguments.enumInput
        return when (enumValue) {
            SomeEnum.VALUE1 -> "SomeEnum.VALUE1"
            null -> "null value"
        }
    }
}
