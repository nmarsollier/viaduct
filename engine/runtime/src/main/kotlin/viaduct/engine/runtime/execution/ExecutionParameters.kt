package viaduct.engine.runtime.execution

import graphql.execution.CoercedVariables
import graphql.execution.ExecutionContext
import graphql.execution.ExecutionStepInfo
import graphql.execution.ExecutionStrategyParameters
import graphql.execution.MergedSelectionSet
import graphql.execution.NonNullableFieldValidator
import graphql.execution.ResultPath
import graphql.schema.GraphQLObjectType
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import viaduct.engine.api.FieldCheckerDispatcherRegistry
import viaduct.engine.api.RawSelectionSet
import viaduct.engine.api.RequiredSelectionSetRegistry
import viaduct.engine.api.TypeCheckerDispatcherRegistry
import viaduct.engine.api.ViaductSchema
import viaduct.engine.api.gj
import viaduct.engine.api.instrumentation.ViaductModernGJInstrumentation
import viaduct.engine.runtime.EngineExecutionContextImpl
import viaduct.engine.runtime.FieldResolutionResult
import viaduct.engine.runtime.ObjectEngineResultImpl
import viaduct.engine.runtime.findLocalContextForType
import viaduct.service.api.spi.FlagManager
import viaduct.service.api.spi.Flags
import viaduct.utils.slf4j.logger

/**
 * Holds parameters used throughout the modern execution strategy.
 *
 * @property executionContext The execution context for the GraphQL query.
 * @property queryPlan The query plan for the current execution.
 * @property engineResult The root object engine result.
 * @property fieldResolutionResult The current engine result, which can be updated as we execute fields.
 * @property rootExecutionJob The root coroutine job for the execution.
 * @property coroutineContext The coroutine context for the current execution.
 * @property executionStepInfo The execution step info for the current execution.
 * @property nonNullFieldValidator The non-null field validator for the current execution.
 * @property requiredSelectionSetRegistry a registry that will be used for loading the data dependencies of a field
 * @property selectionSet The selection set for the current _level_ of execution.
 * @property parent The parent execution parameters, if any.
 * @property field The field currently being executed.
 */
data class ExecutionParameters(
    val executionContext: ExecutionContext,
    val queryPlan: QueryPlan,
    val engineResult: ObjectEngineResultImpl,
    val fieldResolutionResult: FieldResolutionResult,
    val rootExecutionJob: Job,
    val coroutineContext: CoroutineContext,
    val executionStepInfo: ExecutionStepInfo,
    val requiredSelectionSetRegistry: RequiredSelectionSetRegistry,
    val selectionSet: QueryPlan.SelectionSet,
    val rawSelectionSetFactory: RawSelectionSet.Factory,
    val fieldCheckerDispatcherRegistry: FieldCheckerDispatcherRegistry,
    val typeCheckerDispatcherRegistry: TypeCheckerDispatcherRegistry,
    val errorAccumulator: ErrorAccumulator,
    val parent: ExecutionParameters? = null,
    val field: QueryPlan.CollectedField? = null,
) {
    /** The ResultPath for the current level of execution */
    val path: ResultPath get() = executionStepInfo.path

    val gjParameters: ExecutionStrategyParameters =
        ExecutionStrategyParameters.newParameters()
            // graphql-java requires a merged selection set, though our execution strategy doesn't use it.
            // provide a placeholder value
            .fields(emptyMergedSelectionSet)
            .localContext(fieldResolutionResult.localContext)
            .source(fieldResolutionResult.originalSource) // in some cases this should be the resolved one in currentEngineResult
            // nonNullFieldValidator is required but not used in modstrat
            // see [viaduct.engine.runtime.execution.NonNullableFieldValidator]
            .nonNullFieldValidator(NonNullableFieldValidator(executionContext))
            .executionStepInfo(executionStepInfo)
            .path(path)
            .parent(parent?.gjParameters)
            .field(field?.mergedField)
            .build()

    /**
     * The instrumentation instance from the execution context.
     */
    val instrumentation: ViaductModernGJInstrumentation =
        if (executionContext.instrumentation !is ViaductModernGJInstrumentation) {
            ViaductModernGJInstrumentation.fromStandardInstrumentation(executionContext.instrumentation)
        } else {
            executionContext.instrumentation as ViaductModernGJInstrumentation
        }

    /**
     * Launches a coroutine on the root execution scope.
     *
     * @param block The suspend function to execute.
     */
    fun launchOnRootScope(block: suspend CoroutineScope.() -> Unit) =
        CoroutineScope(coroutineContext + rootExecutionJob).launch {
            block(this)
        }

    /**
     * Return a new ExecutionParameters that can be used to execute the given field
     * @param objectType the GraphQLObjectType that owns the definition of `field`
     * @param field the CollectedField to be executed
     */
    fun withCollectedField(
        objectType: GraphQLObjectType,
        field: QueryPlan.CollectedField
    ): ExecutionParameters {
        val coord = objectType.name to field.mergedField.name
        val fieldDef = executionContext.graphQLSchema.getFieldDefinition(coord.gj)
        val key = FieldExecutionHelpers.buildOERKeyForField(this, fieldDef, field)

        val newGjParams = gjParameters.transform {
            it.parent(gjParameters)
            it.path(path.segment(field.responseKey))
            it.field(field.mergedField)
        }
        val executionStepInfo = FieldExecutionHelpers.createExecutionStepInfo(
            newGjParams,
            fieldDef,
            objectType,
            key.arguments
        )
        return copy(
            field = field,
            executionStepInfo = executionStepInfo,
            parent = this,
        )
    }

    /** return a new [ExecutionParameters] that models traversing into the subselections of the provided [field] and its resolved result */
    fun traverseFieldResult(
        field: QueryPlan.CollectedField,
        fieldResolutionResult: FieldResolutionResult
    ): ExecutionParameters {
        val oer = checkNotNull(fieldResolutionResult.engineResult as? ObjectEngineResultImpl)

        return copy(
            // ExecutionStepInfo.type is initially set to an abstract type like Node
            // It can be refined during execution as abstract types become resolved
            executionStepInfo = executionStepInfo.changeTypeWithPreservedNonNull(oer.graphQLObjectType),
            fieldResolutionResult = fieldResolutionResult,
            selectionSet = checkNotNull(field.selectionSet) { "Expected selection set to be non-null." },
        )
    }

    /** return a new [ExecutionParameters] that models pivoting into a childPlan contained by [field] */
    fun traverseChildPlan(
        childPlan: QueryPlan,
        variables: CoercedVariables
    ): ExecutionParameters =
        copy(
            executionContext = executionContext.transform {
                it.coercedVariables(variables)
            },
            queryPlan = childPlan,
            selectionSet = childPlan.selectionSet,
            parent = this,
            errorAccumulator = ErrorAccumulator()
        )

    @Suppress("ktlint:standard:indent")
    class Factory(
        private val requiredSelectionSetRegistry: RequiredSelectionSetRegistry,
        private val fieldCheckerDispatcherRegistry: FieldCheckerDispatcherRegistry,
        private val typeCheckerDispatcherRegistry: TypeCheckerDispatcherRegistry,
        private val flagManager: FlagManager,
    ) {
        private val log by logger()

        /**
         * Constructs [ExecutionParameters] from the execution context and strategy parameters.
         *
         * @param executionContext The execution context for the GraphQL query.
         * @param parameters The execution strategy parameters.
         * @param rootEngineResult The root object engine result.
         * @return A new instance of [ExecutionParameters].
         */
        @OptIn(ExperimentalTime::class)
        internal suspend fun fromExecutionStrategyContextAndParameters(
            executionContext: ExecutionContext,
            parameters: ExecutionStrategyParameters,
            rootEngineResult: ObjectEngineResultImpl,
        ): ExecutionParameters {
            // TODO: determine if we want nested resolvers to be included in this query plan

            val engineExecutionContext = executionContext.findLocalContextForType<EngineExecutionContextImpl>()

            val (queryPlan, duration) = measureTimedValue {
                QueryPlan.build(
                    QueryPlan.Parameters(
                        executionContext.executionInput.query,
                        ViaductSchema(executionContext.graphQLSchema),
                        requiredSelectionSetRegistry,
                        engineExecutionContext.executeAccessChecksInModstrat,
                    ),
                    executionContext.document,
                    executionContext.executionInput.operationName
                        ?.takeIf(String::isNotEmpty)
                        ?.let(DocumentKey::Operation),
                    useCache = !flagManager.isEnabled(Flags.DISABLE_QUERY_PLAN_CACHE)
                )
            }

            val rawSelectionSetFactory = engineExecutionContext.rawSelectionSetFactory

            log.debug("Built QueryPlan in $duration")
            return ExecutionParameters(
                executionContext,
                queryPlan,
                rootEngineResult,
                FieldResolutionResult(rootEngineResult, emptyList(), executionContext.getLocalContext(), emptyMap(), executionContext.getRoot()),
                coroutineContext[Job.Key]!!,
                coroutineContext,
                parameters.executionStepInfo,
                requiredSelectionSetRegistry,
                queryPlan.selectionSet,
                rawSelectionSetFactory,
                fieldCheckerDispatcherRegistry,
                typeCheckerDispatcherRegistry,
                ErrorAccumulator()
            )
        }
    }

    companion object {
        private val emptyMergedSelectionSet = MergedSelectionSet.newMergedSelectionSet().build()
    }
}
