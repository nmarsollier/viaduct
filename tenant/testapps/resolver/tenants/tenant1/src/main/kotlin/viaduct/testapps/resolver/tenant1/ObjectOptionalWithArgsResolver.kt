package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.TestEnum
import viaduct.api.grts.TestObjectValue
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver
class ObjectOptionalWithArgsResolver : QueryResolvers.ObjectOptionalWithArgs() {
    override suspend fun resolve(ctx: Context): TestObjectValue {
        return TestObjectValue.Builder(ctx)
            .strValue(ctx.arguments.strValue ?: "default")
            .optStrValue(ctx.arguments.strValue)
            .enumValue(ctx.arguments.enumValue ?: TestEnum.VALUE1)
            .optEnumValue(ctx.arguments.enumValue)
            .build()
    }
}
