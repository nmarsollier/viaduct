package viaduct.testapps.policycheck.tenant1

import viaduct.api.context.NodeExecutionContext
import viaduct.api.grts.CanAccessPerson
import viaduct.api.internal.NodeResolverBase
import viaduct.api.internal.NodeResolverFor

@NodeResolverFor("CanAccessPerson")
abstract class CanAccessPersonNodeResolverBase : NodeResolverBase<CanAccessPerson> {
    open suspend fun resolve(ctx: Context): CanAccessPerson? = TODO()

    class Context(
        val inner: NodeExecutionContext<CanAccessPerson>
    ) : NodeExecutionContext<CanAccessPerson> by inner
}

class CanAccessPersonNodeResolver : CanAccessPersonNodeResolverBase() {
    override suspend fun resolve(ctx: Context): CanAccessPerson {
        return CanAccessPerson.Builder(ctx.inner).id(ctx.id)
            .name("john").ssn("social security number").build()
    }
}
