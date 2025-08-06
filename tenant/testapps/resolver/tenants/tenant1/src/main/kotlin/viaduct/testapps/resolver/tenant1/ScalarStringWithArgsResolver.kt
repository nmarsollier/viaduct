package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver
class ScalarStringWithArgsResolver : QueryResolvers.ScalarStringWithArgs() {
    override suspend fun resolve(ctx: Context): String {
        return ctx.arguments.input ?: "default"
    }
}
