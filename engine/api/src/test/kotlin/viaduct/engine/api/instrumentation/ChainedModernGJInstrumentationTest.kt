package viaduct.engine.runtime.instrumentation

import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationExecutionStrategyParameters
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import viaduct.engine.api.instrumentation.ChainedModernGJInstrumentation
import viaduct.engine.api.instrumentation.ViaductModernGJInstrumentation

class ChainedModernGJInstrumentationTest {
    @BeforeEach
    fun setUp() = MockKAnnotations.init(this)

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `beginFetchObject delegates to all instrumentations`() {
        val parameters = mockk<InstrumentationExecutionStrategyParameters>()
        val state = mockk<InstrumentationState>()
        val context1 = mockk<InstrumentationContext<Map<String, Any?>>>()
        val context2 = mockk<InstrumentationContext<Map<String, Any?>>>()

        val instr1 = mockk<ViaductModernGJInstrumentation> {
            every { beginFetchObject(parameters, state) } returns context1
        }
        val instr2 = mockk<ViaductModernGJInstrumentation> {
            every { beginFetchObject(parameters, state) } returns context2
        }

        val chained = ChainedModernGJInstrumentation(listOf(instr1, instr2))
        val result = chained.beginFetchObject(parameters, state)

        assertNotNull(result)
        verify { instr1.beginFetchObject(parameters, state) }
        verify { instr2.beginFetchObject(parameters, state) }
    }

    @Test
    fun `beginCompleteObject delegates to all instrumentations`() {
        val parameters = mockk<InstrumentationExecutionStrategyParameters>()
        val state = mockk<InstrumentationState>()
        val context1 = mockk<InstrumentationContext<Any>>()
        val context2 = mockk<InstrumentationContext<Any>>()

        val instr1 = mockk<ViaductModernGJInstrumentation> {
            every { beginCompleteObject(parameters, state) } returns context1
        }
        val instr2 = mockk<ViaductModernGJInstrumentation> {
            every { beginCompleteObject(parameters, state) } returns context2
        }

        val chained = ChainedModernGJInstrumentation(listOf(instr1, instr2))
        val result = chained.beginCompleteObject(parameters, state)

        assertNotNull(result)
        verify { instr1.beginCompleteObject(parameters, state) }
        verify { instr2.beginCompleteObject(parameters, state) }
    }
}
