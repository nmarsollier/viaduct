package viaduct.engine.runtime.execution

import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import io.mockk.every
import io.mockk.mockk
import java.util.function.Supplier
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import viaduct.engine.api.CheckerExecutor
import viaduct.engine.api.CheckerResult
import viaduct.engine.api.CheckerResultContext
import viaduct.engine.api.EngineExecutionContext
import viaduct.engine.api.EngineObjectData
import viaduct.engine.api.RawSelectionSet
import viaduct.engine.api.RequiredSelectionSet
import viaduct.engine.runtime.CompositeLocalContext
import viaduct.engine.runtime.DispatcherRegistry
import viaduct.engine.runtime.EngineExecutionContextImpl
import viaduct.engine.runtime.FieldResolutionResult
import viaduct.engine.runtime.ObjectEngineResultImpl
import viaduct.engine.runtime.Value
import viaduct.engine.runtime.mocks.ContextMocks
import viaduct.engine.runtime.objectEngineResult

@OptIn(ExperimentalCoroutinesApi::class)
class AccessCheckRunnerTest {
    val runner = AccessCheckRunner(DefaultCoroutineInterop)

    @Test
    fun `fieldCheck - flag disabled`() =
        runBlockingTest {
            val result = checkField(false)
            assertEquals(Value.nullValue, result)
        }

    @Test
    fun `fieldCheck - flag enabled, no checker`() =
        runBlockingTest {
            val result = checkField(true)
            assertEquals(Value.nullValue, result)
        }

    @Test
    fun `fieldCheck - flag enabled, checker passes`() =
        runBlockingTest {
            withThreadLocalCoroutineContext {
                val result = checkField(true, successCheckerExecutor)
                assertEquals(CheckerResult.Success, result.await())
            }
        }

    @Test
    fun `fieldCheck - flag enabled, checker fails`() =
        runBlockingTest {
            withThreadLocalCoroutineContext {
                val result = checkField(true, errorCheckerExecutor)
                val error = result.await()?.asError?.error
                assertTrue(error is IllegalAccessException)
                assertEquals("denied", error.message)
            }
        }

    @Test
    fun `typeCheck - flag disabled`() =
        runBlockingTest {
            val result = checkType(false)
            assertEquals(Value.nullValue, result)
        }

    @Test
    fun `typeCheck - flag enabled, no checker`() =
        runBlockingTest {
            val result = checkType(true)
            assertEquals(Value.nullValue, result)
        }

    @Test
    fun `typeCheck - flag enabled, checker passes`() =
        runBlockingTest {
            withThreadLocalCoroutineContext {
                val result = checkType(true, successCheckerExecutor)
                assertEquals(CheckerResult.Success, result.await())
            }
        }

    @Test
    fun `typeCheck - flag enabled, checker fails`() =
        runBlockingTest {
            withThreadLocalCoroutineContext {
                val result = checkType(true, errorCheckerExecutor)
                val error = result.await()?.asError?.error
                assertTrue(error is IllegalAccessException)
                assertEquals("denied", error.message)
            }
        }

    @Test
    fun `combineWithTypeCheck - scalar field`() {
        val result = runner.combineWithTypeCheck(
            Value.fromValue(CheckerResult.Success),
            mockk<GraphQLScalarType>(),
            Value.fromValue(mockk<FieldResolutionResult>()),
            mockk<EngineExecutionContextImpl>()
        )
        assertEquals(Value.fromValue(CheckerResult.Success), result)
    }

    @Test
    fun `combineWithTypeCheck - no type check`() {
        val result = runner.combineWithTypeCheck(
            Value.fromValue(CheckerResult.Success),
            fooObjectType,
            Value.fromValue(mockk<FieldResolutionResult>()),
            ContextMocks(
                myDispatcherRegistry = DispatcherRegistry(emptyMap(), emptyMap(), emptyMap(), emptyMap())
            ).engineExecutionContext as EngineExecutionContextImpl
        )
        assertEquals(Value.fromValue(CheckerResult.Success), result)
    }

    @Test
    fun `combineWithTypeCheck - has type check`() =
        runBlockingTest {
            withThreadLocalCoroutineContext {
                val frr = FieldResolutionResult(
                    engineResult = objectEngineResult {
                        type = fooObjectType
                        data = emptyMap()
                    },
                    emptyList(),
                    ContextMocks().localContext,
                    emptyMap(),
                    null
                )
                val typeChecks = mapOf("Foo" to errorCheckerExecutor)
                val result = runner.combineWithTypeCheck(
                    Value.fromValue(CheckerResult.Success),
                    mockk<GraphQLInterfaceType>(),
                    Value.fromValue(frr),
                    ContextMocks(
                        myDispatcherRegistry = DispatcherRegistry(emptyMap(), emptyMap(), emptyMap(), typeChecks)
                    ).engineExecutionContext as EngineExecutionContextImpl
                )
                val error = result.await()?.asError?.error
                assertTrue(error is IllegalAccessException)
                assertEquals("denied", error.message)
            }
        }

    @Test
    fun `combineWithTypeCheck - has type check but raw value is null`() =
        runBlockingTest {
            withThreadLocalCoroutineContext {
                val frr = FieldResolutionResult(
                    engineResult = null,
                    emptyList(),
                    ContextMocks().localContext,
                    emptyMap(),
                    null
                )
                val typeChecks = mapOf("Foo" to errorCheckerExecutor)
                val result = runner.combineWithTypeCheck(
                    Value.fromValue(CheckerResult.Success),
                    mockk<GraphQLInterfaceType>(),
                    Value.fromValue(frr),
                    ContextMocks(
                        myDispatcherRegistry = DispatcherRegistry(emptyMap(), emptyMap(), emptyMap(), typeChecks)
                    ).engineExecutionContext as EngineExecutionContextImpl
                )
                assertEquals(CheckerResult.Success, result.await())
            }
        }

    private fun checkType(
        isEnabled: Boolean,
        checker: CheckerExecutor? = null
    ): Value<out CheckerResult?> {
        val checkerExecutors = if (checker != null) mapOf("Foo" to checker) else emptyMap()
        val registry = DispatcherRegistry(emptyMap(), emptyMap(), emptyMap(), checkerExecutors)
        val engineExecutionContext = mockk<EngineExecutionContextImpl> {
            every { dispatcherRegistry } returns registry
            every { rawSelectionSetFactory.rawSelectionSet(any(), any()) } returns RawSelectionSet.empty("Foo")
            every { copy(any(), any()) } returns this
            every { executeAccessChecksInModstrat } returns isEnabled
        }
        val oer = objectEngineResult {
            type = mockk { every { name } returns "Foo" }
            data = emptyMap()
        }
        return runner.typeCheck(engineExecutionContext, oer)
    }

    private fun checkField(
        isEnabled: Boolean,
        checker: CheckerExecutor? = null
    ): Value<out CheckerResult?> {
        val exec = AccessCheckRunner(DefaultCoroutineInterop)
        val checkerExecutors = if (checker != null) mapOf("Foo" to "bar" to checker) else emptyMap()
        val registry = DispatcherRegistry(emptyMap(), emptyMap(), checkerExecutors, emptyMap())
        val context = ContextMocks(
            myEngineExecutionContext = mockk<EngineExecutionContextImpl> {
                every { dispatcherRegistry } returns registry
                every { rawSelectionSetFactory.rawSelectionSet(any(), any()) } returns RawSelectionSet.empty("Foo")
                every { copy(any(), any()) } returns this
                every { executeAccessChecksInModstrat } returns isEnabled
            }
        ).localContext
        val params = mockk<ExecutionParameters> {
            every { executionStepInfo } returns mockk {
                every { objectType.name } returns "Foo"
                every { arguments } returns mapOf()
            }
            every { field?.fieldName } returns "bar"
            every { executionContext.getLocalContext<CompositeLocalContext>() } returns context
            every { fieldResolutionResult.engineResult } returns mockk<ObjectEngineResultImpl>()
        }
        val dataFetchingEnvironmentProvider = mockk<Supplier<DataFetchingEnvironment>> {
            every { get() } returns mockk()
        }
        return exec.fieldCheck(params, dataFetchingEnvironmentProvider)
    }

    companion object {
        private val fooObjectType = mockk<GraphQLObjectType> { every { name } returns "Foo" }
        private val successCheckerExecutor = object : CheckerExecutor {
            override val requiredSelectionSets: Map<String, RequiredSelectionSet?> = mapOf()

            override suspend fun execute(
                arguments: Map<String, Any?>,
                objectDataMap: Map<String, EngineObjectData>,
                context: EngineExecutionContext
            ): CheckerResult {
                return CheckerResult.Success
            }
        }

        private val errorCheckerExecutor = object : CheckerExecutor {
            override val requiredSelectionSets: Map<String, RequiredSelectionSet?> = mapOf()

            override suspend fun execute(
                arguments: Map<String, Any?>,
                objectDataMap: Map<String, EngineObjectData>,
                context: EngineExecutionContext
            ): CheckerResult {
                return object : CheckerResult.Error {
                    override val error: Exception = IllegalAccessException("denied")

                    override fun isErrorForResolver(ctx: CheckerResultContext) = true

                    override fun combine(fieldResult: CheckerResult.Error) = this
                }
            }
        }
    }
}
