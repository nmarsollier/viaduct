package viaduct.tenant.runtime.execution

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import viaduct.api.VariablesProvider
import viaduct.api.globalid.GlobalIDCodec
import viaduct.api.internal.ReflectionLoader
import viaduct.api.types.Arguments
import viaduct.engine.api.VariablesResolver
import viaduct.engine.api.mocks.MockEngineObjectData
import viaduct.engine.api.mocks.MockSchema
import viaduct.engine.runtime.mocks.ContextMocks
import viaduct.tenant.runtime.internal.VariablesProviderInfo

class VariablesProviderExecutorTest {
    private data class MockArgs(val args: Map<String, Any?>) : Arguments {
        val a: Int = args["a"] as Int
        val b: Int = args["b"] as Int
    }

    private val objectData = MockEngineObjectData.wrap(MockSchema.minimal.queryType, emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun resolve() =
        runBlockingTest {
            val adapter = VariablesProviderExecutor(
                mockk<GlobalIDCodec>(),
                mockk<ReflectionLoader>(),
                variablesProvider = VariablesProviderInfo(setOf("foo", "bar")) {
                    VariablesProvider<MockArgs> { args ->
                        mapOf("foo" to args.a * 2, "bar" to args.b * 3)
                    }
                }
            ) { args -> MockArgs(args.arguments) }

            assertEquals(
                mapOf("foo" to 10, "bar" to 21),
                adapter.resolve(
                    VariablesResolver.ResolveCtx(
                        objectData,
                        mapOf("a" to 5, "b" to 7),
                        ContextMocks(myFullSchema = MockSchema.minimal).engineExecutionContext
                    )
                )
            )
        }
}
