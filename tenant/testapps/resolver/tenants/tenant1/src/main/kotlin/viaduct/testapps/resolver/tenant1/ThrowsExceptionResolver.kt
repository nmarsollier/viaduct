package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver
class ThrowsExceptionResolver : QueryResolvers.ThrowsException() {
    override suspend fun resolve(ctx: Context): String {
        throw NullPointerException("This is a test exception")
    }
}
