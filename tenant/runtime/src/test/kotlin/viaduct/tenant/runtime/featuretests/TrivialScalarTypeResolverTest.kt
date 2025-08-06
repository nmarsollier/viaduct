package viaduct.tenant.runtime.featuretests

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import viaduct.tenant.runtime.featuretests.fixtures.FeatureTestBuilder

@ExperimentalCoroutinesApi
class TrivialScalarTypeResolverTest {
    @Test
    fun `trivial resolver returns a static value`() {
        FeatureTestBuilder()
            .sdl("type Query { field: Int }")
            .resolver("Query" to "field") { 42 }
            .build()
            .assertJson("{data: {field:42}}", "{field}")
    }
}
