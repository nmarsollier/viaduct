package viaduct.engine.runtime.execution

import graphql.execution.DataFetcherResult
import graphql.execution.ExecutionContext
import graphql.execution.ExecutionStrategyParameters
import graphql.execution.FetchedValue
import kotlinx.coroutines.withContext
import viaduct.engine.api.context.DispatcherLocalContext
import viaduct.engine.runtime.CompositeLocalContext
import viaduct.engine.runtime.EngineResultLocalContext
import viaduct.engine.runtime.ObjectEngineResultImpl
import viaduct.engine.runtime.getLocalContextForType
import viaduct.engine.runtime.updateCompositeLocalContext

suspend inline fun <T> ExecutionContext.executeWithDispatcher(crossinline block: suspend () -> T): T {
    val dispatcherLocalContext = this.executionInput.getLocalContextForType<DispatcherLocalContext>() ?: return block()
    return withContext(dispatcherLocalContext.dispatcher) {
        block()
    }
}

/**
 * create and add an EngineResultLocalContext to this ExecutionStrategyParameters
 * `localContext`, using the provided arguments
 */
fun ExecutionStrategyParameters.withEngineResultLocalContext(
    rootEngineResult: ObjectEngineResultImpl,
    parentEngineResult: ObjectEngineResultImpl,
    queryEngineResult: ObjectEngineResultImpl,
    executionContext: ExecutionContext,
): ExecutionStrategyParameters =
    transform {
        it.localContext(
            updateCompositeLocalContext<EngineResultLocalContext> {
                EngineResultLocalContext(
                    rootEngineResult = rootEngineResult,
                    parentEngineResult = parentEngineResult,
                    queryEngineResult = queryEngineResult,
                    executionContext = executionContext,
                    executionStrategyParams = this
                )
            }
        )
    }

private val Any?.asCompositeLocalContext: CompositeLocalContext
    get() = when (val ctx = this) {
        null -> CompositeLocalContext.empty
        is CompositeLocalContext -> ctx
        else ->
            throw IllegalStateException("Expected CompositeLocalContext but found ${ctx::class}")
    }

/** returns `localContext` as a CompositeLocalContext */
val DataFetcherResult<*>.compositeLocalContext: CompositeLocalContext get() = localContext.asCompositeLocalContext

/** returns `localContext` as a CompositeLocalContext */
val FetchedValue.compositeLocalContext: CompositeLocalContext get() = localContext.asCompositeLocalContext
