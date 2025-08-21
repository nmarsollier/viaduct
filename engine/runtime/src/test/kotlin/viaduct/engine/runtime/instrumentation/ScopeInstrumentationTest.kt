package viaduct.engine.runtime.instrumentation

import graphql.execution.ExecutionContext
import graphql.execution.ExecutionContextBuilder
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.util.function.Consumer
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import viaduct.engine.runtime.EngineExecutionContextImpl
import viaduct.engine.runtime.findLocalContextForType
import viaduct.engine.runtime.isIntrospective
import viaduct.engine.runtime.mkSchema
import viaduct.engine.runtime.mocks.ContextMocks
import viaduct.service.api.spi.FlagManager

internal class ScopeInstrumentationTest {
    private val mockExecutionContext: ExecutionContext = mockk(relaxed = true)

    private val fullSchema = mkSchema(
        """
        type Query {
            helloWorld: String!
        }
        """
    )

    private val scopedSchema = mkSchema(
        """
        type Query {
            helloWorld: String!
        }
        """
    )

    lateinit var contextMocks: ContextMocks

    @BeforeEach
    fun setupMocks() {
        contextMocks = ContextMocks(
            myFullSchema = fullSchema,
            myScopedSchema = scopedSchema,
            myFlagManager = FlagManager.default,
        )

        clearMocks(mockExecutionContext)
    }

    @Test
    fun `Non introspective query should return full Schema`() {
        mockBasics()
        every { mockExecutionContext.isIntrospective } returns false

        ScopeInstrumentation().instrumentExecutionContext(mockExecutionContext, mockk(), null).let {
            val engineExecutionContext = it.findLocalContextForType<EngineExecutionContextImpl>()
            assertEquals(fullSchema, engineExecutionContext.activeSchema)
        }
    }

    @Test
    fun `Introspective query should return scoped Schema`() {
        mockBasics()
        every { mockExecutionContext.isIntrospective } returns true

        ScopeInstrumentation().instrumentExecutionContext(mockExecutionContext, mockk(), null).let {
            val engineExecutionContext = it.findLocalContextForType<EngineExecutionContextImpl>()
            assertEquals(scopedSchema, engineExecutionContext.activeSchema)
        }
    }

    fun mockBasics() {
        every { mockExecutionContext.graphQLSchema } returns contextMocks.engineExecutionContext.activeSchema.schema
        every { mockExecutionContext.getLocalContext<Any>() } returns contextMocks.localContext

        every { mockExecutionContext.transform(any()) } answers {
            val builderConsumer = firstArg<Consumer<ExecutionContextBuilder>>()
            val builder = ExecutionContextBuilder.newExecutionContextBuilder(mockExecutionContext)
            builderConsumer.accept(builder)
            builder.build()
        }
    }
}
