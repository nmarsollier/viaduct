package viaduct.testapps.resolver.tenant3

import viaduct.api.Resolver
import viaduct.testapps.resolver.tenant3.resolverbases.QueryResolvers

@Resolver
class ScalarFailureIntResolver : QueryResolvers.ScalarFailureInt() {
    override suspend fun resolve(ctx: Context): Int {
        return resolve(ctx)
    }
}
