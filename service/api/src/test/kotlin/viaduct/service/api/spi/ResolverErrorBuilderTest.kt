package viaduct.service.api.spi

import graphql.schema.DataFetchingEnvironment
import io.mockk.mockk
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class ResolverErrorBuilderTest {
    @Test
    fun testNoOp() {
        assertNull(
            ResolverErrorBuilder.Companion.NoOpResolverErrorBuilder.exceptionToGraphQLError(
                Throwable("Test Exception"),
                mockk<DataFetchingEnvironment>(),
                ResolverErrorReporter.Companion.ErrorMetadata.EMPTY
            )
        )
    }
}
