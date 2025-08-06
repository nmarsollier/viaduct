package viaduct.engine.runtime.execution

import graphql.ExceptionWhileDataFetching
import graphql.schema.DataFetcher
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import viaduct.engine.runtime.execution.ExecutionTestHelpers.runExecutionTest

@ExperimentalCoroutinesApi
class ExceptionsTest {
    @Test
    fun `simple sync data fetcher`() {
        val exception = execute(
            "{ field }",
            mapOf(
                "Query" to mapOf(
                    "field" to DataFetcher {
                        throw RuntimeException()
                    }
                )
            )
        )
        assertTrue(exception is FieldFetchingException)
    }

    @Test
    fun `simple async data fetcher`() {
        val exception = execute(
            "{ field }",
            mapOf(
                "Query" to mapOf(
                    "field" to DataFetcher {
                        CompletableFuture.supplyAsync { throw RuntimeException() }
                    }
                )
            )
        )
        assertTrue(exception is FieldFetchingException)
    }

    private val defaultSdl = "type Query { field: Int }"

    private fun execute(
        query: String,
        resolvers: Map<String, Map<String, DataFetcher<*>>>,
        sdl: String = defaultSdl
    ): Throwable? =
        runExecutionTest {
            val result = ExecutionTestHelpers.executeViaductModernGraphQL(sdl, resolvers, query)
            result.errors.firstOrNull()?.let {
                (it as ExceptionWhileDataFetching).exception
            }
        }
}
