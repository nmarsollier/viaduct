package viaduct.testapps.hotloading.tenant2

import viaduct.api.Resolver
import viaduct.api.grts.SomeEnum
import viaduct.testapps.hotloading.tenant2.resolverbases.Object5Resolvers

@Resolver(
    """
    fragment _ on Object5 {
        enumField
    }
    """
)
class Object5Field1Resolver : Object5Resolvers.Field1() {
    override suspend fun resolve(ctx: Context): String {
        val enumValue = ctx.objectValue.getEnumField()
        return when (enumValue) {
            SomeEnum.VALUE1 -> "SomeEnum.VALUE1"
            null -> "null value"
        }
    }
}
