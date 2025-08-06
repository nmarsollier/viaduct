package viaduct.engine.runtime.execution

import graphql.execution.DataFetcherResult
import graphql.execution.ExecutionContextBuilder
import graphql.execution.ExecutionId
import graphql.execution.ExecutionStepInfo
import graphql.execution.ExecutionStrategyParameters
import graphql.execution.FetchedValue
import graphql.execution.MergedSelectionSet
import graphql.execution.NonNullableFieldValidator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.engine.api.mocks.MockSchema
import viaduct.engine.runtime.CompositeLocalContext
import viaduct.engine.runtime.EngineResultLocalContext
import viaduct.engine.runtime.ObjectEngineResultImpl
import viaduct.engine.runtime.getLocalContextForType

class GraphQLJavaExtensionsTest {
    @Test
    fun `ExecutionStrategyParameters -- withEngineResultLocalContext`() {
        val schema = MockSchema.mk(
            """
                type Query { x: Int }
                type Foo { x: Int }
            """.trimIndent()
        )
        val context = ExecutionContextBuilder.newExecutionContextBuilder()
            .executionId(ExecutionId.generate())
            .build()
        val params = ExecutionStrategyParameters.newParameters()
            .executionStepInfo(
                ExecutionStepInfo.newExecutionStepInfo().type(schema.queryType)
            )
            .fields(MergedSelectionSet.newMergedSelectionSet().build())
            .nonNullFieldValidator(NonNullableFieldValidator(context))
            .build()
        val root = ObjectEngineResultImpl.newForType(schema.queryType)
        val parent = ObjectEngineResultImpl.newForType(schema.getObjectType("Foo"))
        val query = ObjectEngineResultImpl.newForType(schema.queryType)

        params.withEngineResultLocalContext(root, parent, query, context).let {
            val ctx = it.getLocalContextForType<EngineResultLocalContext>()
            assertNotNull(ctx)
            ctx!!
            assertSame(root, ctx.rootEngineResult)
            assertSame(parent, ctx.parentEngineResult)
            assertSame(query, ctx.queryEngineResult)
        }
    }

    @Test
    fun `FetchedValue -- compositeLocalContext`() {
        // null
        FetchedValue(null, emptyList(), null).let {
            assertEquals(CompositeLocalContext.empty, it.compositeLocalContext)
        }

        // non-null
        FetchedValue(null, emptyList(), CompositeLocalContext.empty).let {
            assertEquals(CompositeLocalContext.empty, it.compositeLocalContext)
        }

        // err
        FetchedValue(null, emptyList(), Unit).let {
            assertThrows<IllegalStateException> {
                it.compositeLocalContext
            }
        }
    }

    @Test
    fun `DataFetcherResult -- compositeLocalContext`() {
        // null
        DataFetcherResult.newResult<Unit>().build().let {
            assertEquals(CompositeLocalContext.empty, it.compositeLocalContext)
        }

        // non-null
        DataFetcherResult.newResult<Unit>()
            .localContext(CompositeLocalContext.empty)
            .build()
            .let {
                assertEquals(CompositeLocalContext.empty, it.compositeLocalContext)
            }

        // err
        DataFetcherResult.newResult<Unit>()
            .localContext(Unit)
            .build()
            .let {
                assertThrows<IllegalStateException> {
                    it.compositeLocalContext
                }
            }
    }
}
