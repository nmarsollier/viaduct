package viaduct.engine.api.execution

import graphql.schema.DataFetchingEnvironment
import io.mockk.mockk
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class ResolverErrorBuilderTest {
    @Test
    fun testNoOp() {
        assertNull(
            ResolverErrorBuilder.NoOpResolverErrorBuilder.exceptionToGraphQLError(
                Throwable("Test Exception"),
                mockk<DataFetchingEnvironment>(),
                ResolverErrorReporter.Companion.ErrorMetadata.EMPTY
            )
        )
    }
}
