package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.TestEnum
import viaduct.api.grts.TestObjectInterface
import viaduct.api.grts.TestObjectValue
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver
class InterfaceValueResolver : QueryResolvers.InterfaceValue() {
    override suspend fun resolve(ctx: Context): TestObjectInterface {
        return TestObjectValue.Builder(ctx)
            .strValue("tenant1 value")
            .enumValue(TestEnum.VALUE1)
            .build()
    }
}
