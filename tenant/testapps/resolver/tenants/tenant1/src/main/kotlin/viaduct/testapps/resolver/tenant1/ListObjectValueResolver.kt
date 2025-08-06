package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.TestEnum
import viaduct.api.grts.TestObjectValue
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver
class ListObjectValueResolver : QueryResolvers.ListObjectValue() {
    override suspend fun resolve(ctx: Context): List<TestObjectValue> {
        return listOf(
            TestObjectValue.Builder(ctx)
                .strValue("value 1")
                .enumValue(TestEnum.VALUE1)
                .build(),
            TestObjectValue.Builder(ctx)
                .strValue("value 2")
                .enumValue(TestEnum.VALUE2)
                .build(),
        )
    }
}
