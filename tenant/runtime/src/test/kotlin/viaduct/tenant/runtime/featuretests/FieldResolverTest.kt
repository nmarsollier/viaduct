package viaduct.tenant.runtime.featuretests

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import viaduct.tenant.runtime.featuretests.fixtures.Baz
import viaduct.tenant.runtime.featuretests.fixtures.FeatureTestBuilder
import viaduct.tenant.runtime.featuretests.fixtures.FeatureTestSchemaFixture
import viaduct.tenant.runtime.featuretests.fixtures.Query
import viaduct.tenant.runtime.featuretests.fixtures.assertData
import viaduct.tenant.runtime.featuretests.fixtures.assertJson

@ExperimentalCoroutinesApi
class FieldResolverTest {
    @Test
    fun `query field resolver throws an exception`() =
        FeatureTestBuilder()
            .sdl("type Query { x: Int }")
            .resolver("Query" to "x") { throw RuntimeException("error!") }
            .build()
            .assertJson(
                """
                {
                    data: { x: null },
                    errors: [{
                        message: "Exception while fetching data (/x) : java.lang.RuntimeException: error!",
                        locations: [{ line: 1, column: 3 }],
                        path: ["x"],
                        extensions: {
                            classification: "DataFetchingException"
                        }
                    }]
                }
                """.trimIndent(),
                "{ x }"
            )

    @Test
    fun `subscription field resolver throws an exception`() =
        FeatureTestBuilder()
            .sdl("type Query { empty: Int } type Subscription { x: Int }")
            .resolver("Subscription" to "x") { throw RuntimeException("error!") }
            .build()
            .assertJson(
                """
                {
                    data: { x: null },
                    errors: [{
                        message: "Exception while fetching data (/x) : java.lang.RuntimeException: error!",
                        locations: [{ line: 1, column: 16 }],
                        path: ["x"],
                        extensions: {
                            classification: "DataFetchingException"
                        }
                    }]
                }
                """.trimIndent(),
                "subscription { x }"
            )

    @Test
    fun `can query a document multiple times with different variables`() =
        // regression, see https://app.asana.com/0/1208357307661305/1209886139365688
        FeatureTestBuilder()
            .sdl("type Query { x:Int, y:Int }")
            .resolver("Query" to "x") { 2 }
            .resolver("Query" to "y") { 3 }
            .build()
            .apply {
                val query = """
                    query Q(${'$'}include_x: Boolean!, ${'$'}include_y: Boolean!) {
                      x @include(if:${'$'}include_x)
                      y @include(if:${'$'}include_y)
                    }
                """.trimIndent()
                assertJson("{data: {x:2}}", query, mapOf("include_x" to true, "include_y" to false))
                assertJson("{data: {y:3}}", query, mapOf("include_x" to false, "include_y" to true))
            }
            .let { Unit } // return Unit

    @Test
    fun `accessing field on node reference throws`() =
        FeatureTestBuilder()
            .grtPackage(Query.Reflection)
            .sdl(FeatureTestSchemaFixture.sdl)
            .resolver("Query" to "baz") { ctx ->
                val id = ctx.globalIDFor(Baz.Reflection, "1")
                ctx.nodeFor(ctx.globalIDFor(Baz.Reflection, "1")).also {
                    assertEquals(id, it.getId())
                    it.getX() // This should throw
                }
            }
            .build()
            .execute("{baz { id }}")
            .assertData("{baz: null}") { errors ->
                assertEquals(1, errors.size)
                val error = errors[0]
                assertTrue(error.message.contains("Attempted to access field Baz.x but it was not set: only id can be accessed on an unresolved Node reference"))
            }
}
