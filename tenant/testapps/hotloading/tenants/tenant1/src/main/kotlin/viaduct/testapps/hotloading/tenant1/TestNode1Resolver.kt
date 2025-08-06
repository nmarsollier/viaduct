package viaduct.testapps.hotloading.tenant1

import viaduct.api.context.NodeExecutionContext
import viaduct.api.grts.TestNode1
import viaduct.api.internal.NodeResolverBase
import viaduct.api.internal.NodeResolverFor

@NodeResolverFor("TestNode1")
abstract class TestNode1NodeResolverBase : NodeResolverBase<TestNode1> {
    open suspend fun resolve(ctx: Context): TestNode1? = TODO()

    class Context(
        val inner: NodeExecutionContext<TestNode1>
    ) : NodeExecutionContext<TestNode1> by inner
}

class TestNode1NodeResolver : TestNode1NodeResolverBase() {
    override suspend fun resolve(ctx: Context): TestNode1 {
        return TestNode1.Builder(ctx.inner).id(ctx.id).key("key").build()
    }
}
