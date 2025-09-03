@file:OptIn(ExperimentalCoroutinesApi::class)

package viaduct.engine.runtime.execution

import graphql.execution.CoercedVariables
import graphql.execution.ExecutionStepInfo
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.engine.runtime.CompositeLocalContext
import viaduct.engine.runtime.FieldResolutionResult
import viaduct.engine.runtime.ObjectEngineResultImpl

class ExecutionParametersTest {
    private val queryObject = mockk<GraphQLObjectType>()
    private val graphQLObject = mockk<GraphQLObjectType>(relaxed = true)

    private fun createMockExecutionParameters(
        variables: CoercedVariables = mockk(),
        source: String = "parentSource",
        executionStepInfo: ExecutionStepInfo = mockk(relaxed = true),
        testForRootQuery: Boolean = true
    ): ExecutionParameters {
        val schemaQueryType = if (testForRootQuery) queryObject else mockk()
        val constants = mockk<ExecutionParameters.Constants>(relaxed = true) {
            every { executionContext.getLocalContext<CompositeLocalContext>() } returns mockk<CompositeLocalContext>()
            every { executionContext.graphQLSchema } returns mockk<GraphQLSchema>(relaxed = true) {
                every { queryType } returns schemaQueryType
            }
            every { queryEngineResult } returns mockk()
            every { executionContext.getRoot<String>() } returns source
        }
        return ExecutionParameters(
            constants = constants,
            parentEngineResult = mockk(),
            coercedVariables = variables,
            queryPlan = mockk(),
            localContext = mockk(relaxed = true),
            source = source,
            executionStepInfo = executionStepInfo,
            selectionSet = mockk(),
            errorAccumulator = mockk()
        )
    }

    @Test
    fun `forFieldChildPlan uses parentEngineResult and parent source for object type`() {
        val expectedSource = "parentSource"
        val expectedVariables = mockk<CoercedVariables>()
        val expectedExecutionStepInfo = mockk<ExecutionStepInfo>(relaxed = true) {
            every { type } returns graphQLObject
            every { path } returns mockk()
        }
        val params = createMockExecutionParameters(
            expectedVariables,
            expectedSource,
            expectedExecutionStepInfo,
            false
        )
        val queryPlan = mockk<QueryPlan> {
            every { parentType } returns graphQLObject
            every { selectionSet } returns mockk()
            every { attribution } returns mockk()
        }

        val result = params.forChildPlan(queryPlan, expectedVariables)
        assertEquals(expectedVariables, result.coercedVariables)
        assertEquals(queryPlan, result.queryPlan)
        assertEquals(queryPlan.selectionSet, result.selectionSet)
        assertEquals(params, result.parent)
        assertEquals(params.parentEngineResult, result.parentEngineResult)
        assertEquals(expectedSource, result.source)
        assertEquals(graphQLObject, result.executionStepInfo.type)
        assertEquals(expectedExecutionStepInfo.path, result.executionStepInfo.path)
    }

    @Test
    fun `forFieldTypeChildPlan throws when child plan does not have object type`() {
        assertThrows<IllegalArgumentException> {
            createMockExecutionParameters().forFieldTypeChildPlan(
                mockk<QueryPlan>(relaxed = true) {
                    every { parentType } returns mockk<GraphQLInterfaceType>()
                },
                mockk(),
                mockk()
            )
        }
    }

    @Test
    fun `forFieldTypeChildPlan throws when fieldResolutionResult does not have engine result`() {
        assertThrows<IllegalArgumentException> {
            createMockExecutionParameters().forFieldTypeChildPlan(
                mockk(relaxed = true),
                mockk(),
                mockk {
                    every { engineResult } returns null
                }
            )
        }
    }

    @Test
    fun `forFieldTypeChildPlan uses parentEngineResult and parent source for object type`() {
        val expectedSource = "testSource"
        val expectedVariables = mockk<CoercedVariables>()
        val expectedExecutionStepInfo = mockk<ExecutionStepInfo>(relaxed = true) {
            every { type } returns graphQLObject
            every { path } returns mockk()
        }
        val params = createMockExecutionParameters(
            expectedVariables,
            expectedSource,
            expectedExecutionStepInfo,
            false
        )
        val queryPlan = mockk<QueryPlan> {
            every { parentType } returns graphQLObject
            every { selectionSet } returns mockk()
            every { attribution } returns mockk()
        }
        val fieldResolutionResult = mockk<FieldResolutionResult> {
            every { engineResult } returns mockk<ObjectEngineResultImpl>()
            every { originalSource } returns expectedSource
        }

        val result = params.forFieldTypeChildPlan(
            queryPlan,
            expectedVariables,
            fieldResolutionResult
        )
        assertEquals(expectedVariables, result.coercedVariables)
        assertEquals(queryPlan, result.queryPlan)
        assertEquals(queryPlan.selectionSet, result.selectionSet)
        assertEquals(params, result.parent)
        assertEquals(fieldResolutionResult.engineResult, result.parentEngineResult)
        assertEquals(fieldResolutionResult.originalSource, result.source)
        assertEquals(graphQLObject, result.executionStepInfo.type)
        assertEquals(expectedExecutionStepInfo.path, result.executionStepInfo.path)
    }
}
