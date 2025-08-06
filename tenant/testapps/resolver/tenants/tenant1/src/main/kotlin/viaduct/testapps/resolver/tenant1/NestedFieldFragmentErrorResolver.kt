package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.Test2ObjectValue
import viaduct.api.grts.TestNestedValue
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver
class NestedFieldFragmentErrorResolver : QueryResolvers.NestedFieldFragmentError() {
    override suspend fun resolve(ctx: Context): TestNestedValue {
        return TestNestedValue.Builder(ctx)
            .value("string value")
            .nestedValue(ctx.objectValue.getThrowsException())
            .nestedObject(Test2ObjectValue.Builder(ctx).value(ctx.objectValue.getThrowsException()).build())
            .build()
    }
}
