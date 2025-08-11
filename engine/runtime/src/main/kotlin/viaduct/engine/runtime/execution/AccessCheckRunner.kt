package viaduct.engine.runtime.execution

import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLCompositeType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import java.util.function.Supplier
import viaduct.engine.api.CheckerDispatcher
import viaduct.engine.api.CheckerResult
import viaduct.engine.api.combine
import viaduct.engine.api.coroutines.CoroutineInterop
import viaduct.engine.runtime.EngineExecutionContextImpl
import viaduct.engine.runtime.FieldResolutionResult
import viaduct.engine.runtime.ObjectEngineResultImpl
import viaduct.engine.runtime.ProxyEngineObjectData
import viaduct.engine.runtime.Value
import viaduct.engine.runtime.findLocalContextForType

/**
 * Helper class that holds logic for executing access checks during field resolution
 */
class AccessCheckRunner(
    private val coroutineInterop: CoroutineInterop,
) {
    /**
     * Executes the field access check for the given field.
     *
     * @param parameters The execution parameters containing field and context information
     * @return [Value] of the [CheckerResult] from executing the checker, or [Value] of null if there is no checker
     */
    fun fieldCheck(
        parameters: ExecutionParameters,
        dataFetchingEnvironmentProvider: Supplier<DataFetchingEnvironment>
    ): Value<out CheckerResult?> {
        val engineExecutionContext = parameters.executionContext.findLocalContextForType<EngineExecutionContextImpl>()
        if (!engineExecutionContext.executeAccessChecksInModstrat) return Value.nullValue

        val field = checkNotNull(parameters.field) { "Expected field to be non-null." }
        val fieldName = field.fieldName
        val parentTypeName = parameters.executionStepInfo.objectType.name
        val localExecutionContext = engineExecutionContext.copy(
            dataFetchingEnvironment = dataFetchingEnvironmentProvider.get()
        )
        val fieldChecker = localExecutionContext.dispatcherRegistry.getFieldCheckerDispatcher(parentTypeName, fieldName)
            ?: return Value.nullValue // No access check for this field, return immediately

        // We're fetching an individual field; the current engine result will always be an ObjectEngineResult
        return executeChecker(fieldChecker, localExecutionContext, parameters.parentEngineResult, parameters.executionStepInfo.arguments)
    }

    /**
     * Executes the type access check for the object type represented by [objectEngineResult].
     *
     * @param objectEngineResult The OER for the object type being checked
     * @return [Value] of the [CheckerResult] from executing the checker, or [Value] of null if there is no checker
     */
    fun typeCheck(
        engineExecutionContext: EngineExecutionContextImpl,
        objectEngineResult: ObjectEngineResultImpl,
    ): Value<out CheckerResult?> {
        if (!engineExecutionContext.executeAccessChecksInModstrat) return Value.nullValue

        val typeName = objectEngineResult.graphQLObjectType.name
        val typeChecker = engineExecutionContext.dispatcherRegistry.getTypeCheckerDispatcher(typeName)
        if (typeChecker == null) {
            // No access check for this field, return immediately
            return Value.nullValue
        }

        // TODO: make this work for shimmed checkers with RSS -- the DFE doesn't work for type checks on list items
        return executeChecker(typeChecker, engineExecutionContext, objectEngineResult, emptyMap())
    }

    private fun executeChecker(
        dispatcher: CheckerDispatcher,
        engineExecutionContext: EngineExecutionContextImpl,
        objectEngineResult: ObjectEngineResultImpl,
        arguments: Map<String, Any?>,
    ): Value<out CheckerResult?> {
        val deferred = coroutineInterop.scopedAsync {
            val rssMap = dispatcher.requiredSelectionSets
            val proxyEODMap = rssMap.mapValues { (_, rss) ->
                val selectionSet = rss?.let {
                    engineExecutionContext.rawSelectionSetFactory.rawSelectionSet(it.selections, emptyMap())
                }
                ProxyEngineObjectData(
                    objectEngineResult,
                    selectionSet,
                    // Bypass access checks for checker required selection sets
                    applyAccessChecks = false
                )
            }
            dispatcher.execute(
                arguments,
                proxyEODMap,
                engineExecutionContext
            )
        }
        return Value.fromDeferred(deferred)
    }

    /**
     * For a given field with type [fieldType], combines the field [CheckerResult] with the type
     * [CheckerResult] if it exists. Prioritizes errors from field checkers over type checkers.
     *
     * @param fieldResolutionResultValue the value in the raw slot of the field
     * @return [Value] of the combined field and type [CheckerResult]s
     */
    fun combineWithTypeCheck(
        fieldCheckerResultValue: Value<out CheckerResult?>,
        fieldType: GraphQLOutputType,
        fieldResolutionResultValue: Value<FieldResolutionResult>,
        engineExecutionContext: EngineExecutionContextImpl
    ): Value<out CheckerResult?> {
        // Exit early if there is definitely no type check
        if (fieldType !is GraphQLCompositeType ||
            (fieldType is GraphQLObjectType && engineExecutionContext.dispatcherRegistry.getTypeCheckerDispatcher(fieldType.name) == null)
        ) {
            return fieldCheckerResultValue
        }

        return fieldResolutionResultValue.flatMap {
            val engineResult = it.engineResult
            if (engineResult != null) {
                val oer = checkNotNull(engineResult as? ObjectEngineResultImpl) {
                    "Expected engineResult to be instance of ObjectEngineResultImpl, got ${engineResult.javaClass}"
                }
                val typeCheckerResultValue = typeCheck(engineExecutionContext, oer)
                when {
                    typeCheckerResultValue == Value.nullValue -> fieldCheckerResultValue
                    fieldCheckerResultValue == Value.nullValue -> typeCheckerResultValue
                    else -> {
                        // Both checkers exist, combine the CheckerResults
                        fieldCheckerResultValue.flatMap { fieldCheckerResult ->
                            typeCheckerResultValue.flatMap { typeCheckerResult ->
                                check(fieldCheckerResult != null && typeCheckerResult != null) { "Expected non-null field and type checker results" }
                                Value.fromValue(typeCheckerResult.combine(fieldCheckerResult)) as Value<out CheckerResult?>
                            }
                        }
                    }
                }
            } else {
                // The raw value resolved to null, don't attempt to execute a type check
                fieldCheckerResultValue
            }
        }
    }
}
