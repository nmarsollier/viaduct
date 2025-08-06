package viaduct.engine.runtime.execution

import graphql.collect.ImmutableMapWithNullValues
import graphql.execution.ExecutionContext
import graphql.execution.ExecutionStepInfo
import graphql.execution.ExecutionStepInfoFactory
import graphql.execution.ExecutionStrategyParameters
import graphql.execution.NormalizedVariables
import graphql.execution.ValuesResolver
import graphql.execution.directives.QueryDirectivesImpl
import graphql.language.Argument
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingEnvironmentImpl
import graphql.schema.DataFetchingFieldSelectionSetImpl
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.util.FpKit
import java.util.function.Supplier
import viaduct.engine.api.EngineExecutionContext
import viaduct.engine.api.ObjectEngineResult
import viaduct.engine.api.RequiredSelectionSet
import viaduct.engine.api.VariablesResolver
import viaduct.engine.api.gj
import viaduct.engine.runtime.EngineResultLocalContext
import viaduct.engine.runtime.ObjectEngineResultImpl
import viaduct.engine.runtime.ProxyEngineObjectData

internal object FieldExecutionHelpers {
    val executionStepInfoFactory = ExecutionStepInfoFactory()

    fun coordinateOfField(
        parameters: ExecutionParameters,
        field: QueryPlan.CollectedField
    ): FieldCoordinates {
        val objectType = parameters.executionStepInfo.objectType
        val fieldName = field.mergedField.name
        return (objectType.name to fieldName).gj
    }

    /**
     * Builds the key for the [ObjectEngineResultImpl] for a given field.
     *
     * @param field The field for which to build the key.
     * @return The constructed key.
     */
    fun buildOERKeyForField(
        parameters: ExecutionParameters,
        field: QueryPlan.CollectedField
    ): ObjectEngineResult.Key =
        buildOERKeyForField(
            parameters,
            parameters.executionStepInfo.fieldDefinition,
            field
        )

    fun buildOERKeyForField(
        parameters: ExecutionParameters,
        def: GraphQLFieldDefinition,
        field: QueryPlan.CollectedField
    ): ObjectEngineResult.Key {
        val schemaArguments = def.arguments
        val keyArguments = if (schemaArguments.isNotEmpty()) {
            getArgumentValues(
                parameters.executionContext,
                schemaArguments,
                field.mergedField.arguments
            ).get()
        } else {
            emptyMap()
        }
        return ObjectEngineResult.Key(field.fieldName, field.alias, keyArguments)
    }

    fun buildDataFetchingEnvironment(
        parameters: ExecutionParameters,
        field: QueryPlan.CollectedField,
        parentOER: ObjectEngineResultImpl,
    ): DataFetchingEnvironment {
        val fieldDef = parameters.executionStepInfo.fieldDefinition
        val execStepInfo = { parameters.executionStepInfo }
        val argumentValuesSupplier = { execStepInfo().arguments }
        val normalizedFieldSupplier = getNormalizedField(parameters.executionContext, parameters.gjParameters, execStepInfo)
        val normalizedVariableValuesSupplier: Supplier<NormalizedVariables> = Supplier {
            // ViaductExecutionStrategy does not use NormalizedVariables, though the GJ interface requires them.
            NormalizedVariables.emptyVariables()
        }
        val fieldCollector = DataFetchingFieldSelectionSetImpl.newCollector(
            parameters.executionContext.graphQLSchema,
            fieldDef.type,
            normalizedFieldSupplier,
        )
        val queryDirectives = QueryDirectivesImpl(
            field.mergedField,
            parameters.executionContext.graphQLSchema,
            parameters.executionContext.coercedVariables,
            normalizedVariableValuesSupplier,
            parameters.executionContext.graphQLContext,
            parameters.executionContext.locale
        )
        val localContext = (parameters.fieldResolutionResult.localContext).let { ctx ->
            ctx.get<EngineResultLocalContext>().let { extant ->
                extant ?: throw IllegalStateException("Missing EngineResultLocalContext")
                ctx.addOrUpdate(extant.copy(parentEngineResult = parentOER))
            }
        }
        return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(parameters.executionContext)
            .source(parameters.fieldResolutionResult.originalSource)
            .localContext(localContext)
            .arguments(argumentValuesSupplier)
            .fieldDefinition(fieldDef)
            .mergedField(field.mergedField)
            .fieldType(fieldDef.type)
            .executionStepInfo(execStepInfo)
            .parentType(parentOER.graphQLObjectType)
            .selectionSet(fieldCollector)
            .queryDirectives(queryDirectives)
            .build()
    }

    fun createExecutionStepInfo(
        parameters: ExecutionStrategyParameters,
        fieldDefinition: GraphQLFieldDefinition,
        fieldContainer: GraphQLObjectType?,
        argumentValues: Map<String, Any?> = emptyMap()
    ): ExecutionStepInfo {
        val field = parameters.field
        val parentStepInfo = parameters.executionStepInfo
        val fieldType = fieldDefinition.type

        return ExecutionStepInfo.newExecutionStepInfo()
            .type(fieldType)
            .fieldDefinition(fieldDefinition)
            .fieldContainer(fieldContainer)
            .field(field)
            .path(parameters.path)
            .parentInfo(parentStepInfo)
            .arguments { ImmutableMapWithNullValues.copyOf(argumentValues) }
            .build()
    }

    internal fun getArgumentValues(
        executionContext: ExecutionContext,
        argDefs: List<GraphQLArgument>,
        args: List<Argument>
    ): Supplier<ImmutableMapWithNullValues<String, Any>> {
        val codeRegistry = executionContext.graphQLSchema.codeRegistry
        val argValuesSupplier = Supplier {
            val resolvedValues = ValuesResolver.getArgumentValues(
                codeRegistry,
                argDefs,
                args,
                executionContext.coercedVariables,
                executionContext.graphQLContext,
                executionContext.locale
            )
            ImmutableMapWithNullValues.copyOf(resolvedValues)
        }
        return FpKit.intraThreadMemoize(argValuesSupplier)
    }

    fun getNormalizedField(
        executionContext: ExecutionContext,
        parameters: ExecutionStrategyParameters,
        executionStepInfo: Supplier<ExecutionStepInfo>
    ): Supplier<ExecutableNormalizedField> {
        val normalizedQuery = executionContext.normalizedQueryTree
        return Supplier {
            normalizedQuery.get().getNormalizedField(
                parameters.field,
                executionStepInfo.get().objectType,
                executionStepInfo.get().path
            )
        }
    }

    /**
     * Run [CollectFields] for the given state
     * @param objectType the current concrete object
     * @param parameters the ExecutionParameters that contains the selection set and
     * variables to be collected
     */
    fun collectFields(
        objectType: GraphQLObjectType,
        parameters: ExecutionParameters
    ): QueryPlan.SelectionSet =
        CollectFields.shallowStrictCollect(
            parameters.selectionSet,
            parameters.executionContext.coercedVariables,
            objectType,
            parameters.queryPlan.fragments
        )

    suspend fun resolveVariables(
        engineResult: ObjectEngineResult,
        requiredSelectionSet: RequiredSelectionSet,
        arguments: Map<String, Any?>,
        ctx: EngineExecutionContext
    ): Map<String, Any?> = resolveVariables(engineResult, requiredSelectionSet.variablesResolvers, arguments, ctx)

    suspend fun resolveVariables(
        engineResult: ObjectEngineResult,
        variablesResolvers: List<VariablesResolver>,
        arguments: Map<String, Any?>,
        ctx: EngineExecutionContext
    ): Map<String, Any?> =
        variablesResolvers.fold(emptyMap()) { acc, vr ->
            val variablesData = vr.requiredSelectionSet?.let { vrss ->
                // VariablesResolvers may have required selection sets which have their own variables resolvers.
                // Recursively resolve them
                val innerVariables = resolveVariables(engineResult, vrss.variablesResolvers, arguments, ctx)
                val vss = ctx.rawSelectionSetFactory.rawSelectionSet(vrss.selections, variables = innerVariables)
                ProxyEngineObjectData(engineResult, vss)
            } ?: ProxyEngineObjectData(engineResult, null)

            val resolved = vr.resolve(VariablesResolver.ResolveCtx(variablesData, arguments, ctx))
            acc + resolved
        }
}
