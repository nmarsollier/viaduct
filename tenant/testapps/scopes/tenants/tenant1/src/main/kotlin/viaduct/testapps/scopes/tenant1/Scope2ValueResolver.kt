package viaduct.testapps.scopes.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.TestScope2Object
import viaduct.testapps.scopes.tenant1.resolverbases.QueryResolvers

@Resolver
class Scope2ValueResolver : QueryResolvers.Scope2Value() {
    override suspend fun resolve(ctx: Context): TestScope2Object {
        return TestScope2Object.Builder(ctx)
            .strValue("scope 2 value")
            .build()
    }
}
