package viaduct.testapps.resolver.tenant3

import viaduct.api.Resolver
import viaduct.testapps.resolver.tenant3.resolverbases.QueryResolvers

@Resolver
class ScalarFailureStringResolver : QueryResolvers.ScalarFailureString() {
    override suspend fun resolve(ctx: Context): String {
        throw RuntimeException("Error Occurred")
    }
}
