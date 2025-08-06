package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.TestObjectValue
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver
class ReturnInvalidNullValueResolver : QueryResolvers.InvalidNullValue() {
    override suspend fun resolve(ctx: Context): TestObjectValue {
        return TestObjectValue.Builder(ctx).build()
    }
}
