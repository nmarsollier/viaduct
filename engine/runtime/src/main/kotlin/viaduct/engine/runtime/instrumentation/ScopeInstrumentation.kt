package viaduct.engine.runtime.instrumentation

import graphql.execution.ExecutionContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import viaduct.engine.api.instrumentation.ViaductInstrumentationBase
import viaduct.engine.runtime.EngineExecutionContextImpl
import viaduct.engine.runtime.findLocalContextForType

class ScopeInstrumentation : ViaductInstrumentationBase() {
    override fun instrumentExecutionContext(
        executionContext: ExecutionContext,
        parameters: InstrumentationExecutionParameters,
        state: InstrumentationState?
    ): ExecutionContext {
        return executionContext.transform {
            // update the execution to use fullSchema, which is safe to do in all cases
            it.graphQLSchema(
                executionContext.findLocalContextForType<EngineExecutionContextImpl>().fullSchema.schema
            )
        }
    }
}
