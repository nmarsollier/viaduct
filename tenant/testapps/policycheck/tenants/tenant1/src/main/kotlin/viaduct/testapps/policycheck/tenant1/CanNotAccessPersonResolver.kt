package viaduct.testapps.policycheck.tenant1

import viaduct.api.context.NodeExecutionContext
import viaduct.api.grts.CanNotAccessPerson
import viaduct.api.internal.NodeResolverBase
import viaduct.api.internal.NodeResolverFor

@NodeResolverFor("CanNotAccessPerson")
abstract class CanNotAccessPersonNodeResolverBase : NodeResolverBase<CanNotAccessPerson> {
    open suspend fun resolve(ctx: Context): CanNotAccessPerson? = TODO()

    class Context(
        val inner: NodeExecutionContext<CanNotAccessPerson>
    ) : NodeExecutionContext<CanNotAccessPerson> by inner
}

class CanNotAccessPersonNodeResolver : CanNotAccessPersonNodeResolverBase() {
    override suspend fun resolve(ctx: Context): CanNotAccessPerson {
        return CanNotAccessPerson.Builder(ctx.inner).id(ctx.id)
            .name("should not resolve").ssn("should not resolve").build()
    }
}
