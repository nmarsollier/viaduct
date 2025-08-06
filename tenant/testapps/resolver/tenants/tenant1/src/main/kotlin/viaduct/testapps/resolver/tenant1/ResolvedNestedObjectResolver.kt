package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.TestObjectValue
import viaduct.api.grts.TestResolvedFieldValue
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers
import viaduct.testapps.resolver.tenant1.resolverbases.TestResolvedFieldValueResolvers

@Resolver
class ResolvedNestedObjectResolver : QueryResolvers.ResolvedNestedObject() {
    override suspend fun resolve(ctx: Context): TestResolvedFieldValue {
        return TestResolvedFieldValue.Builder(ctx)
            .value("string value")
            .build()
    }
}

@Resolver
class ResolvedObjectResolver : TestResolvedFieldValueResolvers.ResolvedObject() {
    override suspend fun resolve(ctx: Context): TestObjectValue {
        return TestObjectValue.Builder(ctx)
            .strValue("tenant1 value")
            .build()
    }
}
