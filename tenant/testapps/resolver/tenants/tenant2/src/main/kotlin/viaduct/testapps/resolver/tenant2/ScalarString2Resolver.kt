package viaduct.testapps.resolver.tenant2

import viaduct.api.Resolver
import viaduct.testapps.resolver.tenant2.resolverbases.QueryResolvers

@Resolver(
    """
        fragment _ on Query {
            privateScalarString
        }
    """
)
class ScalarString2Resolver : QueryResolvers.ScalarString2() {
    override suspend fun resolve(ctx: Context): String {
        val foo = ctx.objectValue.getPrivateScalarString()
        return "resolved: $foo"
    }
}
