package viaduct.engine.api.instrumentation

import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationExecutionStrategyParameters

open class ChainedModernGJInstrumentation(
    val gjInstrumentations: List<ViaductModernGJInstrumentation>
) : ChainedInstrumentation(gjInstrumentations), ViaductModernGJInstrumentation {
    override fun beginFetchObject(
        parameters: InstrumentationExecutionStrategyParameters,
        state: InstrumentationState?
    ): InstrumentationContext<Map<String, Any?>>? =
        ChainedInstrumentationContext(
            gjInstrumentations.map { instr ->
                instr.beginFetchObject(parameters, state)
            }
        )

    override fun beginCompleteObject(
        parameters: InstrumentationExecutionStrategyParameters,
        state: InstrumentationState?
    ): InstrumentationContext<Any>? =
        ChainedInstrumentationContext(
            gjInstrumentations.map { instr ->
                instr.beginCompleteObject(parameters, state)
            }
        )
}
