@file:Suppress("unused", "ClassName")

package viaduct.tenant.runtime.execution.cycles

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.engine.runtime.tenantloading.RequiredSelectionsCycleException
import viaduct.tenant.runtime.execution.cycles.resolverbases.QueryResolvers
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase

class CycleDetectorFeatureAppTest : FeatureAppTestBase() {
    override var sdl = """
        | #START_SCHEMA
        | directive @resolver on FIELD_DEFINITION
        | type Query {
        |   foo:Int @resolver
        |   bar:Int @resolver
        | }
        | #END_SCHEMA
    """.trimMargin()

    @Resolver("fragment _ on Query { bar }")
    class Query_FooResolver : QueryResolvers.Foo() {
        override suspend fun resolve(ctx: Context) = 0
    }

    @Resolver("fragment _ on Query { foo }")
    class Query_BarResolver : QueryResolvers.Bar() {
        override suspend fun resolve(ctx: Context) = 0
    }

    @Test
    fun `cycles in resolver are detected`() {
        var ex: Throwable? = null
        try {
            tryBuildViaductService()
        } catch (e: Exception) {
            ex = e.cause ?: e
        }
        requireNotNull(ex) { "Expected exception during builder build" }
        val cause = generateSequence(ex.cause) { it.cause }
            .firstOrNull { it is RequiredSelectionsCycleException }
        assertTrue(cause is RequiredSelectionsCycleException, "Expected RequiredSelectionsCycleException in cause chain")
    }
}
