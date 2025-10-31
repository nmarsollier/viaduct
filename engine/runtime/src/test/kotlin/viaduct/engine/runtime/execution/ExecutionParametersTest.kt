@file:OptIn(ExperimentalCoroutinesApi::class)

package viaduct.engine.runtime.execution

import graphql.GraphQLContext
import graphql.Scalars.GraphQLID
import graphql.execution.CoercedVariables
import graphql.execution.ExecutionContext
import graphql.execution.ExecutionStepInfo
import graphql.execution.MergedField
import graphql.execution.ResultPath
import graphql.execution.values.InputInterceptor
import graphql.execution.values.legacycoercing.LegacyCoercingInputInterceptor
import graphql.language.Argument as GJArgument
import graphql.language.Field as GJField
import graphql.language.SelectionSet as GJSelectionSet
import graphql.language.StringValue as GJStringValue
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeUtil.unwrapNonNull
import io.mockk.every
import io.mockk.mockk
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.engine.api.ExecutionAttribution
import viaduct.engine.api.FieldCheckerDispatcherRegistry
import viaduct.engine.api.FieldResolverDispatcherRegistry
import viaduct.engine.api.QueryPlanExecutionCondition
import viaduct.engine.api.RequiredSelectionSetRegistry
import viaduct.engine.api.TypeCheckerDispatcherRegistry
import viaduct.engine.api.instrumentation.ViaductModernGJInstrumentation
import viaduct.engine.api.observability.ExecutionObservabilityContext
import viaduct.engine.runtime.CompositeLocalContext
import viaduct.engine.runtime.FieldResolutionResult
import viaduct.engine.runtime.ObjectEngineResultImpl
import viaduct.engine.runtime.execution.ExecutionTestHelpers.createLocalContext
import viaduct.engine.runtime.execution.ExecutionTestHelpers.createSchema

class ExecutionParametersTest {
    private val viaductSchema = createSchema(
        """
        type Query {
            foo(id: ID!): Foo!
        }

        type Foo {
            id: ID!
            name: String!
        }
        """.trimIndent(),
        resolvers = emptyMap()
    )
    private val schema: GraphQLSchema = viaductSchema.schema
    private val fooType: GraphQLObjectType = schema.getObjectType("Foo")
    private val queryType: GraphQLObjectType = schema.queryType
    private val fooFieldDefinition = requireNotNull(queryType.getFieldDefinition("foo"))
    private val emptyVariables = CoercedVariables.of(emptyMap())
    private val defaultRootValue = mapOf("viewerId" to "root")
    private val defaultLocalContext: CompositeLocalContext = createLocalContext(viaductSchema)

    @Test
    fun `forChildPlan uses query engine result for query plans`() {
        val parentSource = mapOf("viewer" to "parent")
        val rootValue = mapOf("viewer" to "root")
        val childPlan = queryPlanFor(
            type = queryType,
            attribution = ExecutionAttribution.fromOperation("RootQuery")
        )
        val parameters = createExecutionParameters(
            source = parentSource,
            executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
                .type(queryType)
                .path(ResultPath.rootPath())
                .build(),
            queryPlan = childPlan,
            parentEngineResult = ObjectEngineResultImpl.newForType(fooType),
            rootValue = rootValue
        )

        val result = parameters.forChildPlan(childPlan, emptyVariables)

        assertSame(parameters.constants.queryEngineResult, result.parentEngineResult)
        assertEquals(rootValue, result.source)
        assertEquals(queryType, result.executionStepInfo.type)
        assertEquals(ResultPath.rootPath(), result.executionStepInfo.path)
        assertSame(parameters, result.parent)
        assertEquals(childPlan.attribution, result.localContext.get<ExecutionObservabilityContext>()?.attribution)
    }

    @Test
    fun `forChildPlan reuses field context for object plans`() {
        val parentSource = mapOf("viewer" to "parent")
        val childAst = selectionSet("name")
        val childPlan = queryPlanFor(
            type = fooType,
            attribution = ExecutionAttribution.fromResolver("FooResolver")
        )
        val mergedField = mergedField("foo", selectionSet("id"))
        val parameters = createExecutionParameters(
            source = parentSource,
            executionStepInfo = executionStepInfoForField(mergedField),
            queryPlan = childPlan,
            parentEngineResult = ObjectEngineResultImpl.newForType(fooType)
        )

        val result = parameters.forChildPlan(childPlan, emptyVariables)

        assertSame(parameters.parentEngineResult, result.parentEngineResult)
        assertEquals(parentSource, result.source)
        assertEquals(fooType, result.executionStepInfo.type)
        assertEquals(ResultPath.rootPath().segment("foo"), result.executionStepInfo.path)
        assertEquals(childPlan.attribution, result.localContext.get<ExecutionObservabilityContext>()?.attribution)
    }

    @Test
    fun `forFieldTypeChildPlan switches to field engine result for object plans`() {
        val parentSource = mapOf("viewer" to "parent")
        val childPlan = queryPlanFor(
            type = fooType,
        )
        val fieldEngineResult = ObjectEngineResultImpl.newForType(fooType)
        val fieldResolutionResult = FieldResolutionResult(
            engineResult = fieldEngineResult,
            errors = emptyList(),
            localContext = CompositeLocalContext.empty,
            extensions = emptyMap(),
            originalSource = mapOf("child" to 1)
        )
        val parameters = createExecutionParameters(
            source = parentSource,
            executionStepInfo = executionStepInfoForField(mergedField("foo", selectionSet("id"))),
            queryPlan = childPlan,
            parentEngineResult = ObjectEngineResultImpl.newForType(fooType)
        )

        val result = parameters.forFieldTypeChildPlan(childPlan, emptyVariables, fieldResolutionResult)

        assertSame(fieldEngineResult, result.parentEngineResult)
        assertEquals(fieldResolutionResult.originalSource, result.source)
        assertEquals(fooType, result.executionStepInfo.type)
    }

    @Test
    fun `forFieldTypeChildPlan uses query engine result for root query plans`() {
        val parentSource = mapOf("viewer" to "parent")
        val childPlan = queryPlanFor(
            type = queryType,
            attribution = ExecutionAttribution.fromOperation("RootChild")
        )
        val parameters = createExecutionParameters(
            source = parentSource,
            executionStepInfo = executionStepInfoForField(mergedField("foo", selectionSet("id"))),
            queryPlan = childPlan
        )
        val fieldResolutionResult = FieldResolutionResult(
            engineResult = null,
            errors = emptyList(),
            localContext = CompositeLocalContext.empty,
            extensions = emptyMap(),
            originalSource = Any()
        )

        val result = parameters.forFieldTypeChildPlan(childPlan, emptyVariables, fieldResolutionResult)

        assertSame(parameters.constants.queryEngineResult, result.parentEngineResult)
        assertEquals(defaultRootValue, result.source)
        assertEquals(ResultPath.rootPath(), result.executionStepInfo.path)
        assertEquals(queryType, result.executionStepInfo.type)
        assertEquals(childPlan.attribution, result.localContext.get<ExecutionObservabilityContext>()?.attribution)
    }

    @Test
    fun `forField builds execution step info for collected field`() {
        val baseParameters = createExecutionParameters(
            source = defaultRootValue,
            executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
                .type(queryType)
                .path(ResultPath.rootPath())
                .build(),
            queryPlan = queryPlanFor(type = queryType)
        )
        val argumentValue = "foo-id"
        val mergedField = mergedField("foo", selectionSet("id"), mapOf("id" to argumentValue))
        val collectedField = collectedFooField(mergedField)

        val result = baseParameters.forField(queryType, collectedField)

        assertSame(baseParameters, result.parent)
        assertSame(baseParameters.parentEngineResult, result.parentEngineResult)
        assertSame(collectedField, result.field)
        assertEquals(ResultPath.rootPath().segment("foo"), result.executionStepInfo.path)
        assertSame(fooFieldDefinition, result.executionStepInfo.fieldDefinition)
        assertSame(queryType, result.executionStepInfo.objectType)
        assertSame(mergedField, result.executionStepInfo.field)
        assertEquals(argumentValue, result.executionStepInfo.arguments["id"])
    }

    @Test
    fun `forObjectTraversal updates engine result local context and type`() {
        val baseParameters = createExecutionParameters(
            source = defaultRootValue,
            executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
                .type(queryType)
                .path(ResultPath.rootPath())
                .build(),
            queryPlan = queryPlanFor(type = queryType)
        )
        val argumentValue = "foo-id"
        val mergedField = mergedField("foo", selectionSet("id"), mapOf("id" to argumentValue))
        val collectedField = collectedFooField(mergedField)
        val fieldParameters = baseParameters.forField(queryType, collectedField)
        val nextEngineResult = ObjectEngineResultImpl.newForType(fooType)
        val updatedLocalContext = fieldParameters.localContext.addOrUpdate(
            ExecutionObservabilityContext(ExecutionAttribution.fromResolver("FooChild"))
        )
        val childSource = mapOf("id" to "foo-1")

        val result = fieldParameters.forObjectTraversal(collectedField, nextEngineResult, updatedLocalContext, childSource)

        assertSame(fieldParameters.parent, result.parent)
        assertSame(fieldParameters.field, result.field)
        assertSame(nextEngineResult, result.parentEngineResult)
        assertSame(updatedLocalContext, result.localContext)
        assertEquals(childSource, result.source)
        assertSame(collectedField.selectionSet, result.selectionSet)
        assertEquals(fieldParameters.executionStepInfo.path, result.executionStepInfo.path)
        assertEquals(nextEngineResult.graphQLObjectType, unwrapNonNull(result.executionStepInfo.type))
    }

    @Test
    fun `forChildPlan throws when plan type is not an object`() {
        val interfacePlan = queryPlanFor(
            type = GraphQLInterfaceType.newInterface()
                .name("Node")
                .field { it.name("id").type(GraphQLID) }
                .build()
        )
        val parameters = createExecutionParameters(
            source = defaultRootValue,
            executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
                .type(queryType)
                .path(ResultPath.rootPath())
                .build(),
            queryPlan = interfacePlan
        )

        assertThrows<IllegalArgumentException> {
            parameters.forChildPlan(interfacePlan, emptyVariables)
        }
    }

    @Test
    fun `forFieldTypeChildPlan throws when field result lacks object engine result`() {
        val childPlan = queryPlanFor(
            type = fooType,
        )
        val invalidFieldResult = FieldResolutionResult(
            engineResult = null,
            errors = emptyList(),
            localContext = CompositeLocalContext.empty,
            extensions = emptyMap(),
            originalSource = Any()
        )
        val parameters = createExecutionParameters(
            source = defaultRootValue,
            executionStepInfo = executionStepInfoForField(mergedField("foo", selectionSet("id"))),
            queryPlan = childPlan
        )

        assertThrows<IllegalStateException> {
            parameters.forFieldTypeChildPlan(childPlan, emptyVariables, invalidFieldResult)
        }
    }

    @Test
    fun `forFieldTypeChildPlan throws when plan type is not an object`() {
        val interfacePlan = queryPlanFor(
            type = GraphQLInterfaceType.newInterface()
                .name("Node")
                .field { it.name("id").type(GraphQLID) }
                .build()
        )
        val parameters = createExecutionParameters(
            source = defaultRootValue,
            executionStepInfo = executionStepInfoForField(mergedField("foo", selectionSet("id"))),
            queryPlan = interfacePlan
        )
        val fieldResolutionResult = FieldResolutionResult(
            engineResult = ObjectEngineResultImpl.newForType(fooType),
            errors = emptyList(),
            localContext = CompositeLocalContext.empty,
            extensions = emptyMap(),
            originalSource = Any()
        )

        assertThrows<IllegalArgumentException> {
            parameters.forFieldTypeChildPlan(interfacePlan, emptyVariables, fieldResolutionResult)
        }
    }

    private fun createExecutionParameters(
        source: Any?,
        executionStepInfo: ExecutionStepInfo,
        queryPlan: QueryPlan,
        parentEngineResult: ObjectEngineResultImpl = ObjectEngineResultImpl.newForType(fooType),
        localContext: CompositeLocalContext = defaultLocalContext,
        rootValue: Any = defaultRootValue,
        queryEngineResult: ObjectEngineResultImpl = ObjectEngineResultImpl.newForType(queryType),
        rootEngineResult: ObjectEngineResultImpl = ObjectEngineResultImpl.newForType(queryType)
    ): ExecutionParameters {
        val executionContext = executionContext(rootValue, localContext)
        val constants = ExecutionParameters.Constants(
            executionContext = executionContext,
            rootEngineResult = rootEngineResult,
            queryEngineResult = queryEngineResult,
            rootExecutionJob = Job(),
            coroutineContext = EmptyCoroutineContext,
            requiredSelectionSetRegistry = RequiredSelectionSetRegistry.Empty,
            rawSelectionSetFactory = mockk(relaxed = true),
            fieldCheckerDispatcherRegistry = FieldCheckerDispatcherRegistry.Empty,
            typeCheckerDispatcherRegistry = TypeCheckerDispatcherRegistry.Empty,
            fieldResolverDispatcherRegistry = FieldResolverDispatcherRegistry.Empty
        )
        return ExecutionParameters(
            constants = constants,
            parentEngineResult = parentEngineResult,
            coercedVariables = emptyVariables,
            queryPlan = queryPlan,
            localContext = localContext,
            source = source,
            executionStepInfo = executionStepInfo,
            selectionSet = queryPlan.selectionSet,
            errorAccumulator = ErrorAccumulator()
        )
    }

    private fun executionContext(
        rootValue: Any?,
        localContext: CompositeLocalContext
    ): ExecutionContext {
        val gqlContext = GraphQLContext.newContext()
            .of(InputInterceptor::class.java, LegacyCoercingInputInterceptor.migratesValues())
            .build()
        val instrumentation = mockk<ViaductModernGJInstrumentation>(relaxed = true)
        val executionContext = mockk<ExecutionContext>(relaxed = true)
        every { executionContext.graphQLSchema } returns schema
        every { executionContext.getRoot<Any>() } returns rootValue
        every { executionContext.getLocalContext<CompositeLocalContext?>() } returns localContext
        every { executionContext.instrumentation } returns instrumentation
        every { executionContext.transform(any()) } answers { executionContext }
        every { executionContext.graphQLContext } returns gqlContext
        return executionContext
    }

    private fun queryPlanFor(
        type: GraphQLOutputType,
        attribution: ExecutionAttribution? = ExecutionAttribution.DEFAULT
    ): QueryPlan =
        QueryPlan(
            selectionSet = QueryPlan.SelectionSet.empty,
            fragments = QueryPlan.Fragments.empty,
            variablesResolvers = emptyList(),
            parentType = type,
            childPlans = emptyList(),
            attribution = attribution,
            executionCondition = QueryPlanExecutionCondition.ALWAYS_EXECUTE
        )

    private fun executionStepInfoForField(field: MergedField): ExecutionStepInfo =
        ExecutionStepInfo.newExecutionStepInfo()
            .type(fooFieldDefinition.type)
            .fieldDefinition(fooFieldDefinition)
            .fieldContainer(queryType)
            .field(field)
            .path(ResultPath.rootPath().segment(fooFieldDefinition.name))
            .build()

    private fun mergedField(
        name: String,
        selectionSet: GJSelectionSet? = null,
        arguments: Map<String, String> = emptyMap()
    ): MergedField {
        val fieldBuilder = GJField.newField(name)
        fieldBuilder.arguments(
            arguments.map {
                GJArgument.newArgument()
                    .name(it.key)
                    .value(GJStringValue(it.value))
                    .build()
            }
        )
        selectionSet?.let { fieldBuilder.selectionSet(it) }
        return MergedField.newMergedField(fieldBuilder.build()).build()
    }

    private fun selectionSet(vararg fields: String): GJSelectionSet {
        val builder = GJSelectionSet.newSelectionSet()
        fields.forEach { builder.selection(GJField.newField(it).build()) }
        return builder.build()
    }

    private fun collectedFooField(
        mergedField: MergedField,
        selectionSet: QueryPlan.SelectionSet = QueryPlan.SelectionSet.empty
    ): QueryPlan.CollectedField =
        QueryPlan.CollectedField(
            responseKey = mergedField.name,
            selectionSet = selectionSet,
            mergedField = mergedField,
            childPlans = emptyList(),
            fieldTypeChildPlans = emptyMap()
        )
}
