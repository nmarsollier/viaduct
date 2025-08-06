package viaduct.testapps.resolver.tenant3

import viaduct.api.Resolver
import viaduct.api.grts.TestEnum
import viaduct.testapps.resolver.tenant3.resolverbases.QueryResolvers

@Resolver
class ScalarFailureEnumResolver : QueryResolvers.ScalarFailureEnum() {
    override suspend fun resolve(ctx: Context): TestEnum {
        return null!!
    }
}
