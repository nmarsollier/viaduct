package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.TestEnum
import viaduct.api.grts.TestObjectValue
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver
class ObjectValueResolver : QueryResolvers.ObjectValue() {
    override suspend fun resolve(ctx: Context): TestObjectValue {
        return TestObjectValue.Builder(ctx)
            .strValue("tenant1 value")
            .optStrValue("tenant1 optional value")
            .enumValue(TestEnum.VALUE1)
            .optEnumValue(TestEnum.VALUE2)
            .build()
    }
}
