package viaduct.tenant.runtime.featuretests

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import viaduct.tenant.runtime.featuretests.fixtures.FeatureTestBuilder

@ExperimentalCoroutinesApi
class CodeHotloadingTest {
    @Test
    fun `basic`() {
        val builder = FeatureTestBuilder()
            .sdl("extend type Query { a: Int }")
            .resolver("Query" to "a") { 1 }
        val viaductTest = builder.build()
        viaductTest.assertJson("{data: {a: 1}}", "{ a }")
        builder
            .resolver("Query" to "a") { 2 }
            .build()
            .assertJson("{ data: {a: 2}}", "{ a }")
    }
}
