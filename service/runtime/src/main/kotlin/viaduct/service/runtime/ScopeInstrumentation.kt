package viaduct.service.runtime

import graphql.execution.ExecutionContext
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.schema.GraphQLSchema
import javax.inject.Inject
import viaduct.engine.runtime.EngineExecutionContextImpl
import viaduct.engine.runtime.findLocalContextForType
import viaduct.service.api.spi.FlagManager
import viaduct.service.api.spi.Flags

@Suppress("ktlint:standard:indent")
class ScopeInstrumentation
    @Inject
    constructor(
        private val flagManager: FlagManager
    ) : Instrumentation {
        override fun instrumentExecutionContext(
            executionContext: ExecutionContext,
            parameters: InstrumentationExecutionParameters,
            state: InstrumentationState?
        ): ExecutionContext {
            return executionContext.transform {
                it.graphQLSchema(getGraphQLSchema(executionContext) ?: executionContext.graphQLSchema)
            }
        }

        private fun isModernExecutionStrategyEnabled(executionContext: ExecutionContext) =
            flagManager.isEnabled(
                Flags.useModernExecutionStrategy(executionContext.operationDefinition.operation)
            )

        private fun getGraphQLSchema(executionContext: ExecutionContext): GraphQLSchema? =
            if (isModernExecutionStrategyEnabled(executionContext)) {
                executionContext.findLocalContextForType<EngineExecutionContextImpl>().fullSchema.schema
            } else {
                null
            }
    }
