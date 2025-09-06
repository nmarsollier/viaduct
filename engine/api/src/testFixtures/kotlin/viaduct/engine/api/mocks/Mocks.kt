@file:Suppress("ForbiddenImport")

package viaduct.engine.api.mocks

import graphql.execution.AsyncExecutionStrategy
import graphql.execution.ExecutionStrategy
import graphql.execution.SimpleDataFetcherExceptionHandler
import graphql.execution.instrumentation.ChainedInstrumentation
import graphql.execution.instrumentation.Instrumentation
import graphql.schema.GraphQLCompositeType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import viaduct.dataloader.mocks.MockNextTickDispatcher
import viaduct.engine.api.CheckerExecutor
import viaduct.engine.api.CheckerExecutorFactory
import viaduct.engine.api.CheckerResult
import viaduct.engine.api.CheckerResultContext
import viaduct.engine.api.Coordinate
import viaduct.engine.api.EngineExecutionContext
import viaduct.engine.api.EngineObjectData
import viaduct.engine.api.FieldResolverDispatcher
import viaduct.engine.api.FieldResolverDispatcherRegistry
import viaduct.engine.api.FieldResolverExecutor
import viaduct.engine.api.NodeResolverExecutor
import viaduct.engine.api.ParsedSelections
import viaduct.engine.api.RawSelectionSet
import viaduct.engine.api.RequiredSelectionSet
import viaduct.engine.api.RequiredSelectionSetRegistry
import viaduct.engine.api.TenantAPIBootstrapper
import viaduct.engine.api.TenantModuleBootstrapper
import viaduct.engine.api.VariablesResolver
import viaduct.engine.api.ViaductSchema
import viaduct.engine.api.coroutines.CoroutineInterop
import viaduct.engine.api.mocks.MockRequiredSelectionSetRegistry.RequiredSelectionSetEntry.FieldEntry
import viaduct.engine.api.select.SelectionsParser
import viaduct.engine.runtime.CheckerDispatcherImpl
import viaduct.engine.runtime.DispatcherRegistry
import viaduct.engine.runtime.FieldResolverDispatcherImpl
import viaduct.engine.runtime.NodeResolverDispatcherImpl
import viaduct.engine.runtime.execution.DefaultCoroutineInterop
import viaduct.engine.runtime.mocks.ContextMocks
import viaduct.engine.runtime.select.RawSelectionSetFactoryImpl
import viaduct.engine.runtime.select.RawSelectionSetImpl
import viaduct.service.runtime.ViaductWiringFactory

typealias CheckerFn = suspend (arguments: Map<String, Any?>, objectDataMap: Map<String, EngineObjectData>) -> Unit
typealias NodeBatchResolverFn = suspend (selectors: List<NodeResolverExecutor.Selector>, context: EngineExecutionContext) -> Map<NodeResolverExecutor.Selector, Result<EngineObjectData>>
typealias NodeUnbatchedResolverFn = (id: String, selections: RawSelectionSet?, context: EngineExecutionContext) -> EngineObjectData
typealias FieldUnbatchedResolverFn = suspend (
    arguments: Map<String, Any?>,
    objectValue: EngineObjectData,
    queryValue: EngineObjectData,
    selections: RawSelectionSet?,
    context: EngineExecutionContext
) -> Any?

typealias FieldBatchResolverFn = suspend (selectors: List<FieldResolverExecutor.Selector>, context: EngineExecutionContext) -> Map<FieldResolverExecutor.Selector, Result<Any?>>
typealias VariablesResolverFn = suspend (ctx: VariablesResolver.ResolveCtx) -> Map<String, Any?>

fun mkCoroutineInterop(): CoroutineInterop = DefaultCoroutineInterop

fun mkExecutionStrategy(): ExecutionStrategy = AsyncExecutionStrategy(SimpleDataFetcherExceptionHandler())

fun mkInstrumentation(): Instrumentation = ChainedInstrumentation(listOf<Instrumentation>())

fun RawSelectionSet.variables() = (this as RawSelectionSetImpl).ctx.variables

fun mkRawSelectionSet(
    parsedSelections: ParsedSelections,
    viaductSchema: ViaductSchema,
    variables: Map<String, Any?>
): RawSelectionSet =
    RawSelectionSetImpl.create(
        parsedSelections,
        variables,
        viaductSchema
    )

fun mkRawSelectionSetFactory(viaductSchema: ViaductSchema) = RawSelectionSetFactoryImpl(viaductSchema)

fun mkRSS(
    typeName: String,
    selectionString: String,
    variableProviders: List<VariablesResolver> = emptyList()
) = RequiredSelectionSet(SelectionsParser.parse(typeName, selectionString), variableProviders)

class MockRequiredSelectionSetRegistry constructor(
    val entries: List<RequiredSelectionSetEntry> = emptyList()
) : RequiredSelectionSetRegistry {
    sealed class RequiredSelectionSetEntry {
        abstract val selectionsType: String
        abstract val selectionsString: String
        abstract val variableProviders: List<VariablesResolver>

        abstract class FieldEntry(
            val coord: Coordinate,
        ) : RequiredSelectionSetEntry()

        /**
         * A RequiredSelectionSet entry for a specific coordinate's resolver.
         */
        class FieldResolverEntry(
            coord: Coordinate,
            override val selectionsType: String,
            override val selectionsString: String,
            override val variableProviders: List<VariablesResolver>
        ) : FieldEntry(coord) {
            constructor(
                coord: Coordinate,
                selections: String,
                variableProviders: List<VariablesResolver> = emptyList()
            ) : this(
                coord = coord,
                selectionsType = coord.first,
                selectionsString = selections,
                variableProviders = variableProviders
            )
        }

        /**
         * A RequiredSelectionSet entry for a specific coordinate's checker.
         */
        class FieldCheckerEntry(
            coord: Coordinate,
            override val selectionsType: String,
            override val selectionsString: String,
            override val variableProviders: List<VariablesResolver>
        ) : FieldEntry(coord) {
            constructor(
                coord: Coordinate,
                selections: String,
                variableProviders: List<VariablesResolver> = emptyList()
            ) : this(
                coord = coord,
                selectionsType = coord.first,
                selectionsString = selections,
                variableProviders = variableProviders
            )
        }

        /**
         * A RequiredSelectionSet entry for a specific type checker.
         */
        data class TypeCheckerEntry(
            val typeName: String,
            override val selectionsType: String,
            override val selectionsString: String,
            override val variableProviders: List<VariablesResolver>
        ) : RequiredSelectionSetEntry() {
            constructor(
                typeName: String,
                selections: String,
                variableProviders: List<VariablesResolver> = emptyList()
            ) : this(
                typeName = typeName,
                selectionsType = typeName,
                selectionsString = selections,
                variableProviders = variableProviders
            )
        }
    }

    /** merge this registry with the provided registry */
    operator fun plus(other: MockRequiredSelectionSetRegistry): MockRequiredSelectionSetRegistry = MockRequiredSelectionSetRegistry(other.entries + entries)

    companion object {
        val empty: MockRequiredSelectionSetRegistry = MockRequiredSelectionSetRegistry()

        fun mk(vararg entries: RequiredSelectionSetEntry): MockRequiredSelectionSetRegistry = MockRequiredSelectionSetRegistry(entries.toList())

        /**
         * Create a MockRequiredSelectionSetRegistry for a table of entries that do not use variables.
         *
         * This method be used to compactly initialize a registry.
         *
         * When fieldName is `null`, it will be interpreted as a selection on the type itself.
         *
         * Example:
         * ```
         *   MockRequiredSelectionSetRegistry.mk(
         *     "Type" to "foo" to "requiredSelection",
         *     "Type" to "bar" to "requiredSelection",
         *     "Type" to null to "requiredSelection"
         *   )
         * ```
         */
        fun mk(vararg entries: Pair<Pair<String, String?>, String>): MockRequiredSelectionSetRegistry =
            MockRequiredSelectionSetRegistry(
                entries.map { it ->
                    val coordPair = it.first
                    coordPair.second?.let { fieldName ->
                        RequiredSelectionSetEntry.FieldResolverEntry(
                            coord = Coordinate(coordPair.first, fieldName),
                            selectionsType = coordPair.first,
                            selectionsString = it.second,
                            variableProviders = emptyList()
                        )
                    } ?: RequiredSelectionSetEntry.TypeCheckerEntry(
                        typeName = coordPair.first,
                        selectionsType = coordPair.first,
                        selectionsString = it.second,
                        variableProviders = emptyList()
                    )
                }
            )

        /**
         * Create a MockRequiredSelectionSetRegistry for a table of entries that use variables.
         * Selection strings will be interpreted to be object selections on the coordinate's type.
         *
         * Example:
         * ```
         *   MockRequiredSelectionSetRegistry.mk(
         *     "Type" to "foo" to "requiredSelection" to variablesResolver,
         *     "Type" to "bar" to "requiredSelection" to variablesResolver,
         *     "Type" to null to "requiredSelection" to variablesResolver
         *   )
         * ```
         */
        @JvmName("mkWithVariables1")
        fun mk(vararg entries: Pair<Pair<Pair<String, String?>, String>, VariablesResolver>): MockRequiredSelectionSetRegistry =
            MockRequiredSelectionSetRegistry(
                entries.map {
                    val coordPair = it.first.first
                    coordPair.second?.let { fieldName ->
                        RequiredSelectionSetEntry.FieldResolverEntry(
                            coord = Coordinate(coordPair.first, fieldName),
                            selectionsType = coordPair.first,
                            selectionsString = it.first.second,
                            variableProviders = listOf(it.second)
                        )
                    } ?: RequiredSelectionSetEntry.TypeCheckerEntry(
                        typeName = coordPair.first,
                        selectionsType = coordPair.first,
                        selectionsString = it.first.second,
                        variableProviders = listOf(it.second)
                    )
                }
            )

        /**
         * Create a MockRequiredSelectionSetRegistry for a table of entries that use variables.
         * Selection strings will be interpreted to be object selections on the coordinate's type.
         *
         * Example:
         * ```
         *   MockRequiredSelectionSetRegistry.mk(
         *     "Type" to "foo" to "requiredSelection" to variablesResolvers,
         *     "Type" to "bar" to "requiredSelection" to variablesResolvers,
         *     "Type" to null to "requiredSelection" to variablesResolvers
         *   )
         * ```
         */
        @JvmName("mkWithVariables2")
        fun mk(vararg entries: Pair<Pair<Pair<String, String?>, String>, List<VariablesResolver>>): MockRequiredSelectionSetRegistry =
            entries.fold(empty) { acc, e ->
                val coordPair = e.first.first
                acc + mkForSelectedType(coordPair.first, e)
            }

        /**
         * Create a MockRequiredSelectionSetRegistry for a table of entries, where all selectionStrings are
         * selections on the provided [typeName]
         *
         * Example:
         * ```
         *   MockRequiredSelectionSetRegistry.mkForType(
         *     "Query",
         *     "Type" to "foo" to "fieldOnQuery",
         *     "Type" to null to "fieldOnQuery",
         *   )
         * ```
         */
        fun mkForSelectedType(
            typeName: String,
            vararg entries: Pair<Pair<String, String?>, String>
        ): MockRequiredSelectionSetRegistry =
            MockRequiredSelectionSetRegistry(
                entries.map {
                    val coordPair = it.first
                    coordPair.second?.let { fieldName ->
                        RequiredSelectionSetEntry.FieldResolverEntry(
                            coord = Coordinate(coordPair.first, fieldName),
                            selectionsType = typeName,
                            selectionsString = it.second,
                            variableProviders = emptyList()
                        )
                    } ?: RequiredSelectionSetEntry.TypeCheckerEntry(
                        typeName = coordPair.first,
                        selectionsType = typeName,
                        selectionsString = it.second,
                        variableProviders = emptyList()
                    )
                }
            )

        /**
         * Create a MockRequiredSelectionSetRegistry for a table of entries, where all selectionStrings are
         * selections on the provided [typeName]
         *
         * Example:
         * ```
         *   MockRequiredSelectionSetRegistry.mkForType(
         *     "Query",
         *     "Type" to "foo" to "fieldOnQuery" to variablesResolvers,
         *     "Type" to null to "fieldOnQuery" to variablesResolvers,
         *   )
         * ```
         */
        @JvmName("mkForTypeWithVariables")
        fun mkForSelectedType(
            typeName: String,
            vararg entries: Pair<Pair<Pair<String, String?>, String>, List<VariablesResolver>>
        ): MockRequiredSelectionSetRegistry =
            MockRequiredSelectionSetRegistry(
                entries.map {
                    val coordPair = it.first.first
                    coordPair.second?.let { fieldName ->
                        RequiredSelectionSetEntry.FieldResolverEntry(
                            coord = Coordinate(coordPair.first, fieldName),
                            selectionsType = typeName,
                            selectionsString = it.first.second,
                            variableProviders = it.second
                        )
                    } ?: RequiredSelectionSetEntry.TypeCheckerEntry(
                        typeName = coordPair.first,
                        selectionsType = typeName,
                        selectionsString = it.first.second,
                        variableProviders = it.second
                    )
                }
            )
    }

    override fun getFieldResolverRequiredSelectionSets(
        typeName: String,
        fieldName: String,
    ): List<RequiredSelectionSet> =
        entries
            .filterIsInstance<RequiredSelectionSetEntry.FieldResolverEntry>()
            .filter { it.coord == (typeName to fieldName) }
            .map { mkRSS(it.selectionsType, it.selectionsString, it.variableProviders) }

    final override fun getFieldCheckerRequiredSelectionSets(
        typeName: String,
        fieldName: String,
        executeAccessChecksInModstrat: Boolean
    ): List<RequiredSelectionSet> =
        entries
            .filterIsInstance<RequiredSelectionSetEntry.FieldCheckerEntry>()
            .filter { it.coord == (typeName to fieldName) }
            .map { mkRSS(it.selectionsType, it.selectionsString, it.variableProviders) }

    fun getRequiredSelectionSetsForField(
        typeName: String,
        fieldName: String
    ): List<RequiredSelectionSet> = getRequiredSelectionSetsForField(typeName, fieldName, true)

    /**
     * Overrides the original getRequiredSelectionSetsForType method and exposes a new one without
     * `executeAccessChecksInModstrat` as it is not relevant for the mock implementation.
     */
    override fun getRequiredSelectionSetsForType(
        typeName: String,
        executeAccessChecksInModstrat: Boolean
    ): List<RequiredSelectionSet> = getRequiredSelectionSetsForType(typeName)

    fun getRequiredSelectionSetsForType(typeName: String): List<RequiredSelectionSet> =
        entries
            .filterIsInstance<RequiredSelectionSetEntry.TypeCheckerEntry>()
            .filter { it.typeName == typeName }
            .map { mkRSS(it.selectionsType, it.selectionsString, it.variableProviders) }
}

class MockVariablesResolver(vararg names: String, val resolveFn: VariablesResolverFn) : VariablesResolver {
    override val variableNames: Set<String> = names.toSet()

    override suspend fun resolve(ctx: VariablesResolver.ResolveCtx): Map<String, Any?> = resolveFn(ctx)
}

/**
 * Create a [ViaductSchema] with mock wiring, which allows for schema parsing and validation.
 * This is useful for testing local changes that are unnecessary for a full engine execution,
 * e.g., unit tests.
 *
 * @param sdl The SDL string to parse and create the schema.
 */
fun mkSchema(sdl: String): ViaductSchema {
    val tdr = SchemaParser().parse(sdl)
    return ViaductSchema(SchemaGenerator().makeExecutableSchema(tdr, RuntimeWiring.MOCKED_WIRING))
}

/**
 * Create a [ViaductSchema] with actual wiring, which allows for real execution.
 * This is useful for testing the actual engine behaviors, e.g., engine feature test.
 *
 * @param sdl The SDL string to parse and create the schema.
 */
fun mkSchemaWithWiring(sdl: String): ViaductSchema {
    val tdr = SchemaParser().parse(sdl)
    val actualWiringFactory = ViaductWiringFactory(DefaultCoroutineInterop)
    val wiring = RuntimeWiring.newRuntimeWiring().wiringFactory(actualWiringFactory).build()

    // Let SchemaProblem and other GraphQL validation errors pass through
    return ViaductSchema(SchemaGenerator().makeExecutableSchema(tdr, wiring))
}

object MockSchema {
    val minimal: ViaductSchema = mkSchema("type Query { empty: Int }")

    fun mk(sdl: String) = mkSchema(sdl)
}

fun mkDispatcherRegistry(
    fieldResolverExecutors: Map<Coordinate, FieldResolverExecutor> = emptyMap(),
    nodeResolverExecutors: Map<String, NodeResolverExecutor> = emptyMap(),
    fieldCheckerExecutors: Map<Coordinate, CheckerExecutor> = emptyMap(),
    typeCheckerExecutors: Map<String, CheckerExecutor> = emptyMap(),
): DispatcherRegistry {
    return DispatcherRegistry(
        fieldResolverDispatchers = fieldResolverExecutors.map { (k, v) -> k to FieldResolverDispatcherImpl(v) }.toMap(),
        nodeResolverDispatchers = nodeResolverExecutors.map { (k, v) -> k to NodeResolverDispatcherImpl(v) }.toMap(),
        fieldCheckerDispatchers = fieldCheckerExecutors.map { (k, v) -> k to CheckerDispatcherImpl(v) }.toMap(),
        typeCheckerDispatchers = typeCheckerExecutors.map { (k, v) -> k to CheckerDispatcherImpl(v) }.toMap(),
    )
}

class MockFieldResolverDispatcherRegistry(vararg bindings: Pair<Coordinate, FieldResolverDispatcher>) : FieldResolverDispatcherRegistry {
    private val bindingsMap = bindings.toMap()

    override fun getFieldResolverDispatcher(
        typeName: String,
        fieldName: String
    ): FieldResolverDispatcher? = bindingsMap[typeName to fieldName]
}

open class MockFieldUnbatchedResolverExecutor(
    override val objectSelectionSet: RequiredSelectionSet? = null,
    override val querySelectionSet: RequiredSelectionSet? = null,
    override val metadata: Map<String, String> = emptyMap(),
    override val resolverId: String,
    open val unbatchedResolveFn: FieldUnbatchedResolverFn = { _, _, _, _, _ -> null },
) : FieldResolverExecutor {
    override val isBatching: Boolean = false

    override suspend fun batchResolve(
        selectors: List<FieldResolverExecutor.Selector>,
        context: EngineExecutionContext
    ): Map<FieldResolverExecutor.Selector, Result<Any?>> {
        require(selectors.size == 1) { "Unbatched resolver should only receive single selector, got {}".format(selectors.size) }
        val selector = selectors.first()
        return mapOf(selector to runCatching { unbatchedResolveFn(selector.arguments, selector.objectValue, selector.queryValue, selector.selections, context) })
    }

    companion object {
        /** a [FieldResolverExecutor] implementation that always returns `null` */
        val Null: MockFieldUnbatchedResolverExecutor = MockFieldUnbatchedResolverExecutor(resolverId = "") { _, _, _, _, _ -> null }
    }
}

open class MockFieldBatchResolverExecutor(
    override val objectSelectionSet: RequiredSelectionSet? = null,
    override val querySelectionSet: RequiredSelectionSet? = null,
    override val metadata: Map<String, String> = emptyMap(),
    override val resolverId: String,
    open val batchResolveFn: FieldBatchResolverFn = { _, _ -> throw NotImplementedError() }
) : FieldResolverExecutor {
    override val isBatching: Boolean = true

    override suspend fun batchResolve(
        selectors: List<FieldResolverExecutor.Selector>,
        context: EngineExecutionContext
    ): Map<FieldResolverExecutor.Selector, Result<Any?>> = batchResolveFn(selectors, context)
}

@OptIn(ExperimentalCoroutinesApi::class)
fun FieldResolverExecutor.invoke(
    fullSchema: ViaductSchema,
    coord: Coordinate,
    arguments: Map<String, Any?> = emptyMap(),
    objectValue: Map<String, Any?> = emptyMap(),
    queryValue: Map<String, Any?> = emptyMap(),
    selections: RawSelectionSet? = null,
    context: EngineExecutionContext = ContextMocks(fullSchema).engineExecutionContext,
) = runBlocking(MockNextTickDispatcher()) {
    val selector = FieldResolverExecutor.Selector(
        arguments = arguments,
        objectValue = MockEngineObjectData(fullSchema.schema.getObjectType(coord.first), objectValue),
        queryValue = MockEngineObjectData(fullSchema.schema.queryType, queryValue),
        selections = selections,
    )
    batchResolve(listOf(selector), context)[selector]?.getOrNull()
}

@OptIn(ExperimentalCoroutinesApi::class)
fun CheckerExecutor.invoke(
    fullSchema: ViaductSchema,
    coord: Coordinate,
    arguments: Map<String, Any?> = emptyMap(),
    objectDataMap: Map<String, Map<String, Any?>> = emptyMap(),
    context: EngineExecutionContext = ContextMocks(fullSchema).engineExecutionContext,
) = runBlocking(MockNextTickDispatcher()) {
    val objectType = fullSchema.schema.getObjectType(coord.first)!!
    val objectMap = objectDataMap.mapValues { (_, it) -> MockEngineObjectData(objectType, it) }
    execute(arguments, objectMap, context)
}

class MockCheckerErrorResult(override val error: Exception) : CheckerResult.Error {
    override fun isErrorForResolver(ctx: CheckerResultContext): Boolean {
        return true
    }

    override fun combine(fieldResult: CheckerResult.Error): CheckerResult.Error {
        return fieldResult
    }
}

class MockCheckerExecutor(
    override val requiredSelectionSets: Map<String, RequiredSelectionSet?> = emptyMap(),
    val executeFn: CheckerFn = { _, _ -> }
) : CheckerExecutor {
    override suspend fun execute(
        arguments: Map<String, Any?>,
        objectDataMap: Map<String, EngineObjectData>,
        context: EngineExecutionContext
    ): CheckerResult {
        try {
            executeFn(arguments, objectDataMap)
        } catch (e: Exception) {
            return MockCheckerErrorResult(e)
        }
        return CheckerResult.Success
    }
}

class MockNodeUnbatchedResolverExecutor(
    override val typeName: String = "MockNode",
    val unbatchedResolveFn: NodeUnbatchedResolverFn = { _, _, _ -> throw NotImplementedError() }
) : NodeResolverExecutor {
    override val isBatching: Boolean = false

    override suspend fun batchResolve(
        selectors: List<NodeResolverExecutor.Selector>,
        context: EngineExecutionContext
    ): Map<NodeResolverExecutor.Selector, Result<EngineObjectData>> {
        return selectors.associateWith { selector ->
            try {
                Result.success(unbatchedResolveFn(selector.id, selector.selections, context))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

class MockNodeBatchResolverExecutor(
    override val typeName: String,
    val batchResolveFn: NodeBatchResolverFn = { _, _ -> throw NotImplementedError() }
) : NodeResolverExecutor {
    override val isBatching: Boolean = true

    override suspend fun batchResolve(
        selectors: List<NodeResolverExecutor.Selector>,
        context: EngineExecutionContext
    ): Map<NodeResolverExecutor.Selector, Result<EngineObjectData>> = batchResolveFn(selectors, context)
}

class MockTenantAPIBootstrapper(
    val tenantModuleBootstrappers: List<TenantModuleBootstrapper> = emptyList()
) : TenantAPIBootstrapper {
    override suspend fun tenantModuleBootstrappers(): Iterable<TenantModuleBootstrapper> = tenantModuleBootstrappers
}

class MockTenantModuleBootstrapper(
    val schema: ViaductSchema,
    val fieldResolverExecutors: Iterable<Pair<Coordinate, FieldResolverExecutor>> = emptyList(),
    val nodeResolverExecutors: Iterable<Pair<String, NodeResolverExecutor>> = emptyList(),
    val checkerExecutors: Map<Coordinate, CheckerExecutor> = emptyMap(),
    val typeCheckerExecutors: Map<String, CheckerExecutor> = emptyMap(),
) : TenantModuleBootstrapper {
    override fun fieldResolverExecutors(schema: ViaductSchema): Iterable<Pair<Coordinate, FieldResolverExecutor>> = fieldResolverExecutors

    override fun nodeResolverExecutors(schema: ViaductSchema): Iterable<Pair<String, NodeResolverExecutor>> = nodeResolverExecutors

    fun resolverAt(coord: Coordinate) = fieldResolverExecutors(schema).first { it.first == coord }.second

    fun checkerAt(coord: Coordinate) = checkerExecutors[coord]

    companion object {
        /**
         * Create a [MockTenantModuleBootstrapper] with the provided schema SDL.
         * This will parse the SDL and create a [ViaductSchema] with actual wiring.
         */
        operator fun invoke(
            schemaSDL: String,
            block: MockTenantModuleBootstrapperDSL<Unit>.() -> Unit
        ) = invoke(mkSchemaWithWiring(schemaSDL), block)

        /**
         * Create a [MockTenantModuleBootstrapper] with the provided [ViaductSchema].
         * The provided schema should already be built with actual wiring via `mkSchemaWithWiring`,
         * not `mkSchema` with mock wiring.
         */
        operator fun invoke(
            schemaWithWiring: ViaductSchema,
            block: MockTenantModuleBootstrapperDSL<Unit>.() -> Unit
        ) = MockTenantModuleBootstrapperDSL(schemaWithWiring, Unit).apply { block() }.create()
    }

    fun resolveField(
        coord: Coordinate,
        arguments: Map<String, Any?> = emptyMap(),
        objectValue: Map<String, Any?> = emptyMap(),
        queryValue: Map<String, Any?> = emptyMap(),
        selections: RawSelectionSet? = null,
        context: EngineExecutionContext = contextMocks.engineExecutionContext,
    ) = resolverAt(coord).invoke(schema, coord, arguments, objectValue, queryValue, selections, context)

    fun checkField(
        coord: Coordinate,
        arguments: Map<String, Any?> = emptyMap(),
        objectDataMap: Map<String, Map<String, Any?>> = emptyMap(),
        context: EngineExecutionContext = contextMocks.engineExecutionContext,
    ) = checkerAt(coord)!!.invoke(schema, coord, arguments, objectDataMap, context)

    fun toDispatcherRegistry(
        checkerExecutors: Map<Coordinate, CheckerExecutor>? = null,
        typeCheckerExecutors: Map<String, CheckerExecutor>? = null
    ): DispatcherRegistry =
        mkDispatcherRegistry(
            fieldResolverExecutors.toMap(),
            nodeResolverExecutors.toMap(),
            checkerExecutors ?: this.checkerExecutors,
            typeCheckerExecutors ?: this.typeCheckerExecutors,
        )

    val contextMocks by lazy {
        ContextMocks(
            myFullSchema = schema,
            myDispatcherRegistry = this.toDispatcherRegistry(),
        )
    }
}

data class MockEngineObjectData(override val graphQLObjectType: GraphQLObjectType, val data: Map<String, Any?>) : EngineObjectData {
    override suspend fun fetch(selection: String): Any? = data[selection]

    companion object {
        /** recursively wraps [data] into a MockEngineObjectData tree */
        fun wrap(
            graphQLObjectType: GraphQLObjectType,
            data: Map<String, Any?>
        ): MockEngineObjectData = maybeWrap(graphQLObjectType, data) as MockEngineObjectData

        private fun maybeWrap(
            type: GraphQLOutputType,
            value: Any?
        ): Any? =
            if (value == null) {
                null
            } else {
                @Suppress("UNCHECKED_CAST")
                when (type) {
                    is GraphQLNonNull -> maybeWrap(type.wrappedType as GraphQLOutputType, value)
                    is GraphQLList -> (value as List<*>).map { maybeWrap(type.wrappedType as GraphQLOutputType, it) }
                    is GraphQLObjectType ->
                        MockEngineObjectData(
                            type,
                            (value as Map<String, Any?>).mapValues { (fname, value) ->
                                maybeWrap(type.getFieldDefinition(fname).type, value)
                            }
                        )

                    is GraphQLCompositeType -> throw IllegalArgumentException("don't know how to wrap type $type with value $value")
                    else -> value
                }
            }
    }
}

class MockCheckerExecutorFactory(
    val checkerExecutors: Map<Coordinate, CheckerExecutor>? = null,
    val typeCheckerExecutors: Map<String, CheckerExecutor>? = null
) : CheckerExecutorFactory {
    override fun checkerExecutorForField(
        typeName: String,
        fieldName: String
    ): CheckerExecutor? {
        return checkerExecutors?.get(Pair(typeName, fieldName))
    }

    override fun checkerExecutorForType(typeName: String): CheckerExecutor? {
        return typeCheckerExecutors?.get(typeName)
    }
}

object Samples {
    val testSchema = mkSchemaWithWiring(
        """
        type Query {
            foo: String
        }
        interface Node { id: ID! }
        type TestType {
            aField: String
            bIntField: Int
            parameterizedField(experiment: Boolean): Boolean
            cField(f1: String, f2: Int): String
            dField: String
            batchField: String
        }
        type TestNode implements Node { id: ID! }
        type TestBatchNode implements Node { id: ID! }
        """.trimIndent()
    )

    val mockTenantModule = MockTenantModuleBootstrapper(testSchema) {
        // Add resolver for aField
        field("TestType" to "aField") {
            resolver {
                fn { _, _, _, _, _ -> "aField" }
            }
        }

        // Add resolver for bIntField
        field("TestType" to "bIntField") {
            resolver {
                fn { _, _, _, _, _ -> 42 }
            }
        }

        // Add resolver for parameterizedField with a required selection set
        field("TestType" to "parameterizedField") {
            resolver {
                objectSelections("fragment _ on TestType { aField @include(if: \$experiment) bIntField }") {
                    variables("experiment") { ctx ->
                        mapOf("experiment" to (ctx.arguments["experiment"] ?: false))
                    }
                }
                fn { args, _, _, _, _ -> args["experiment"] as? Boolean ?: false }
            }
        }

        // Add resolver for cField
        field("TestType" to "cField") {
            resolver {
                fn { _, _, _, _, _ -> "cField" }
            }
        }

        // Add resolver for dField with variable provider
        field("TestType" to "dField") {
            resolver {
                objectSelections("fragment _ on TestType { aField @include(if: \$experiment) bIntField }") {
                    variables("experiment") { _ ->
                        mapOf("experiment" to true)
                    }
                }
                fn { _, _, _, _, _ -> "dField" }
            }
        }

        // Add batch resolver for batchField
        field("TestType" to "batchField") {
            resolver {
                fn { _, _ -> mapOf() }
            }
        }

        // Add node resolver for TestNode
        type("TestNode") {
            nodeUnbatchedExecutor { id, _, _ ->
                MockEngineObjectData(
                    testSchema.schema.getObjectType("TestNode"),
                    mapOf("id" to id)
                )
            }
        }

        // Add a batch node resolver for TestBatchNode
        type("TestBatchNode") {
            nodeBatchedExecutor { selectors, _ ->
                selectors.associateWith { selector ->
                    Result.success(
                        MockEngineObjectData(
                            testSchema.schema.getObjectType("TestBatchNode"),
                            mapOf("id" to selector.id)
                        )
                    )
                }
            }
        }
    }
}
