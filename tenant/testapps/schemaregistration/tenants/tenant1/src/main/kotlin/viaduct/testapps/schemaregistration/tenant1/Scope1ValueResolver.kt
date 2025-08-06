package viaduct.testapps.schemaregistration.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.TestScope1Object
import viaduct.testapps.schemaregistration.tenant1.resolverbases.QueryResolvers

@Resolver
class Scope1ValueResolver : QueryResolvers.Scope1Value() {
    override suspend fun resolve(ctx: Context): TestScope1Object {
        return TestScope1Object.Builder(ctx)
            .strValue("scope 1 value")
            .build()
    }
}
