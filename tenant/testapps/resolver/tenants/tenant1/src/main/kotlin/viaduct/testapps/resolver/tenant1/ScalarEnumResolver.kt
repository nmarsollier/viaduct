package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.TestEnum
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver
class ScalarEnumResolver : QueryResolvers.ScalarEnum() {
    override suspend fun resolve(ctx: Context): TestEnum {
        return TestEnum.VALUE1
    }
}
