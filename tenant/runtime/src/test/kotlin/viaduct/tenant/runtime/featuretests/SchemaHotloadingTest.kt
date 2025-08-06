package viaduct.tenant.runtime.featuretests

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import viaduct.tenant.runtime.featuretests.fixtures.FeatureTestBuilder
import viaduct.tenant.runtime.featuretests.fixtures.UntypedFieldContext

/**
 * Unit tests that covers the cases documented in RFC-178.
 * For e2e test of schema hotswap, please see `SchemaHotloadingE2ETest` in projects/viaduct/oss/testapps/hotloading.
 */
@ExperimentalCoroutinesApi
class SchemaHotloadingTest {
    @Test
    fun `new nullable field returns null`() =
        FeatureTestBuilder()
            .sdl("type Query { a: Int }")
            .resolver("Query" to "a") { 1 }
            .build()
            .changeSchema("type Query { a: Int, b: Int }")
            .assertJson(
                """
                {
                    data: { a: 1, b: null },
                }
                """.trimIndent(),
                "{ a b }"
            )

    @Test
    fun `remove field with field resolver returns validation error`() =
        FeatureTestBuilder()
            .sdl("type Query { a: Int, b: Int }")
            .resolver("Query" to "a") { 1 }
            .resolver("Query" to "b") { 2 }
            .build()
            .changeSchema("type Query { a: Int }")
            .assertJson(
                """
                {
                  "errors": [
                    {
                      "message": "Validation error (FieldUndefined@[b]) : Field 'b' in type 'Query' is undefined",
                      "locations": [
                        {
                          "line": 1,
                          "column": 5
                        }
                      ],
                      "extensions": {
                        "classification": "ValidationError"
                      }
                    }
                  ],
                  "data": null
                }
                """.trimIndent(),
                "{ a b }"
            )

    @Test
    fun `change field type to a whole new type returns field level DataFetchingException`() =
        FeatureTestBuilder()
            .sdl("type Query { a: String, b: String } ")
            .resolver("Query" to "a") { "abc" }
            .resolver("Query" to "b") { "def" }
            .build()
            .changeSchema("type Query { a: [String], b: String }")
            .assertJson(
                """
                {
                  "errors": [
                    {
                      "message": "Exception while fetching data (/a) : Expected data to be an Iterable, was class java.lang.String.",
                      "locations": [
                        {
                          "line": 1,
                          "column": 3
                        }
                      ],
                       "path": [
                            "a"
                        ],
                      "extensions": {
                        "classification": "DataFetchingException"
                      }
                    }
                  ],
                  "data": {
                    "a": null,
                    "b": "def"
                  }
                }
                """.trimIndent(),
                "{ a b }"
            )

    @Test
    fun `change field type from non-nullable to nullable`() =
        FeatureTestBuilder()
            .sdl("type Query { a: String!, b: String } ")
            .resolver("Query" to "a") { "abc" }
            .resolver(
                "Query" to "b",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<String>("a", String::class) + "def" },
                objectValueFragment = "a"
            )
            .build()
            .changeSchema("type Query { a: String, b: String }")
            .assertJson(
                """
                {
                  "data": {
                    "a": "abc",
                    "b": "abcdef"
                  }
                }
                """.trimIndent(),
                "{ a b }"
            )
}
