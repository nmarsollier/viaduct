package viaduct.engine.runtime.execution

import com.github.benmanes.caffeine.cache.Caffeine
import graphql.execution.MergedField
import graphql.language.AstPrinter
import graphql.language.Document
import graphql.language.Field as GJField
import graphql.language.FragmentDefinition as GJFragmentDefinition
import graphql.language.FragmentSpread as GJFragmentSpread
import graphql.language.InlineFragment as GJInlineFragment
import graphql.language.NodeUtil
import graphql.language.OperationDefinition
import graphql.language.SelectionSet as GJSelectionSet
import graphql.language.SourceLocation
import graphql.schema.GraphQLCompositeType
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLTypeUtil
import java.util.concurrent.Executors
import kotlin.jvm.optionals.getOrNull
import kotlinx.coroutines.future.await
import viaduct.engine.api.RequiredSelectionSetRegistry
import viaduct.engine.api.VariablesResolver
import viaduct.engine.api.ViaductSchema
import viaduct.engine.api.gj
import viaduct.engine.runtime.execution.QueryPlan.Field

/**
 * QueryPlan is an intermediate representation of a GraphQL selection set.
 * It includes models of viaduct-specific concepts, including required selection sets
 * and their variables.
 */
data class QueryPlan(
    val selectionSet: SelectionSet,
    val fragments: Fragments,
    val variablesResolvers: List<VariablesResolver>,
    val parentType: GraphQLOutputType
) {
    /**
     * Configuration for building a QueryPlan.
     *
     * @property schema GraphQL schema used for type verification and field resolution
     */
    data class Parameters(
        val query: String,
        val schema: ViaductSchema,
        val registry: RequiredSelectionSetRegistry,
        val executeAccessChecksInModstrat: Boolean
    )

    /**
     * A Selection models any kind of element that may appear in a QueryPlan SelectionSet.
     *
     * Selection comes in some of the same flavors as graphql-java's [graphql.language.Selection],
     * though with the significant inclusion of CollectedField.
     */
    sealed interface Selection {
        val constraints: Constraints
    }

    /**
     * A CollectedField is the result of applying the CollectFields algorithm.
     *
     * It represents a merged and normalized selection within a selection set, and has
     * no unresolved constraints like unapplied conditional directives, and will always be executed.
     */
    data class CollectedField(
        val responseKey: String,
        val selectionSet: SelectionSet?,
        val mergedField: MergedField,
        val childPlans: List<QueryPlan>,
    ) : Selection {
        override val constraints: Constraints get() = Constraints.Unconstrained
        val sourceLocation: SourceLocation get() = mergedField.singleField.sourceLocation
        val fieldName: String get() = mergedField.name
        val alias: String? get() = mergedField.singleField.alias

        override fun toString(): String = AstPrinter.printAst(mergedField.singleField)
    }

    /**
     * [Selection] also has representations similar to graphql-java's [graphql.language.Selection] classes.
     *
     * These selections have not been collected yet and may be subject to [Constraints]
     * that determine if/how they get collected.
     */
    class Field(
        val resultKey: String,
        override val constraints: Constraints,
        val field: GJField,
        val selectionSet: SelectionSet?,
        val childPlans: List<QueryPlan>
    ) : Selection {
        override fun toString(): String = AstPrinter.printAst(field)
    }

    data class FragmentSpread(
        val name: String,
        override val constraints: Constraints
    ) : Selection

    data class InlineFragment(
        val selectionSet: SelectionSet,
        override val constraints: Constraints
    ) : Selection

    data class FragmentDefinition(val selectionSet: SelectionSet)

    data class Fragments(val map: Map<String, FragmentDefinition>) : Map<String, FragmentDefinition> by map {
        operator fun plus(other: Fragments): Fragments = copy(map + other.map)

        operator fun plus(entry: Pair<String, FragmentDefinition>): Fragments = copy(map + entry)

        companion object {
            val empty: Fragments = Fragments(emptyMap())
        }
    }

    data class SelectionSet(val selections: List<Selection>) {
        operator fun plus(selection: Selection): SelectionSet = copy(selections = selections + selection)

        companion object {
            val empty: SelectionSet = SelectionSet(emptyList())
        }
    }

    companion object {
        private val queryPlanBuilderExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
        )
            // ensure that threads in this pool are created as daemons, otherwise this thread pool
            // prevents graceful shutdown in ExecutionBenchmark
            { runnable ->
                Executors.defaultThreadFactory().newThread(runnable).also { it.setDaemon(true) }
            }

        private val queryPlanCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .executor(queryPlanBuilderExecutor)
            .buildAsync<QueryPlanCacheKey, QueryPlan>()

        private data class QueryPlanCacheKey(val documentText: String, val documentKey: DocumentKey, val schemaHashCode: Int, val executeAccessChecksInModstrat: Boolean)

        fun resetCache() {
            queryPlanCache.synchronous().invalidateAll()
        }

        internal val cacheSize: Long get() = queryPlanCache.synchronous().estimatedSize()

        /**
         * Builds a [QueryPlan] from a GraphQL [Document].
         *
         * @param parameters The parameters containing the schema.
         * @param document The GraphQL document.
         * @param documentKey A pointer into [document] describing the element to build a QueryPlan around
         * @return A new [QueryPlan] instance.
         */
        suspend fun build(
            parameters: Parameters,
            document: Document,
            documentKey: DocumentKey? = null,
            useCache: Boolean = true,
        ): QueryPlan {
            val fragmentsByName = NodeUtil.getFragmentsByName(document)
            val operations = document.getDefinitionsOfType(OperationDefinition::class.java)

            val docKey = documentKey
                ?: operations.firstOrNull()?.let { DocumentKey.Operation(it.name) }
                ?: document.getFirstDefinitionOfType(GJFragmentDefinition::class.java).getOrNull()?.let { DocumentKey.Fragment(it.name) }
                ?: throw IllegalStateException("document contains no fragment or operation definitions")

            val (selectionSet, parentType) = when (val key = docKey) {
                is DocumentKey.Operation -> {
                    val maybeOp =
                        if (key.name != null) {
                            document.getOperationDefinition(key.name).getOrNull()
                        } else {
                            document.getFirstDefinitionOfType(OperationDefinition::class.java).getOrNull()
                        }
                    val op = checkNotNull(maybeOp) {
                        "Operation `${key.name}` not found in document"
                    }
                    op.selectionSet to getParentTypeFromDefinition(parameters, op)
                }

                is DocumentKey.Fragment -> {
                    val frag = checkNotNull(fragmentsByName[key.name]) {
                        "Fragment `${key.name}` not found in document"
                    }
                    frag.selectionSet to getParentTypeFromDefinition(parameters, frag)
                }
            }

            return build(parameters, selectionSet, docKey, parentType, fragmentsByName, useCache)
        }

        /**
         * Determines the parent GraphQL type based on the given definition.
         *
         * @param parameters The parameters containing the schema.
         * @param definition The operation or fragment definition.
         * @return The parent [GraphQLCompositeType].
         */
        private fun getParentTypeFromDefinition(
            parameters: Parameters,
            definition: Any,
        ): GraphQLCompositeType {
            return when (definition) {
                is OperationDefinition -> when (definition.operation) {
                    OperationDefinition.Operation.QUERY -> parameters.schema.schema.queryType
                    OperationDefinition.Operation.MUTATION -> parameters.schema.schema.mutationType
                    OperationDefinition.Operation.SUBSCRIPTION -> parameters.schema.schema.subscriptionType
                    else -> throw IllegalStateException("Unsupported operation type: ${definition.operation}")
                }

                is GJFragmentDefinition -> parameters.schema.schema.getType(definition.typeCondition.name) as? GraphQLCompositeType
                    ?: throw IllegalStateException("Type ${definition.typeCondition.name} not found in schema.")

                else -> throw IllegalArgumentException("Unsupported definition type: ${definition::class}")
            }
        }

        /**
         * Builds a [QueryPlan] from a selection set, parent type, and fragments.
         *
         * @param parameters The parameters containing the schema.
         * @param selectionSet The selection set.
         * @param parentType The parent GraphQL type.
         * @param fragmentsByName A map of fragment definitions by name.
         * @return A new [QueryPlan] instance.
         */
        private suspend fun build(
            parameters: Parameters,
            selectionSet: GJSelectionSet,
            documentKey: DocumentKey,
            parentType: GraphQLCompositeType,
            fragmentsByName: Map<String, GJFragmentDefinition>,
            useCache: Boolean = true,
        ): QueryPlan {
            fun build(): QueryPlan =
                QueryPlanBuilder(parameters, fragmentsByName, emptyList())
                    .build(selectionSet, parentType)

            return if (useCache) {
                val cacheKey = QueryPlanCacheKey(parameters.query, documentKey, parameters.schema.hashCode(), parameters.executeAccessChecksInModstrat)
                queryPlanCache.get(cacheKey) { _ -> build() }.await()
            } else {
                build()
            }
        }
    }
}

/**
 * A stateful builder for QueryPlan. Instances of [QueryPlanBuilder] should only be used to build
 * a single QueryPlan.
 *
 * @param variablesResolvers: a list of [VariablesResolver]s.
 *   Any variable reference encountered by this builder must correspond to exactly one VariablesResolver
 */
private class QueryPlanBuilder(
    private val parameters: QueryPlan.Parameters,
    private val fragmentsByName: Map<String, GJFragmentDefinition>,
    private val variablesResolvers: List<VariablesResolver>
) {
    private val fragments: MutableMap<String, QueryPlan.FragmentDefinition> = mutableMapOf()

    private data class State(
        val selectionSet: QueryPlan.SelectionSet,
        val parentType: GraphQLCompositeType,
        val constraints: Constraints
    )

    // Builders may cache results that are only valid for the specific input they were
    // created for, and are likely unsafe to reuse.
    // Guard against reuse
    private var built = false

    fun build(
        selectionSet: GJSelectionSet,
        parentType: GraphQLCompositeType,
    ): QueryPlan {
        return createQueryPlan(selectionSet, parentType)
    }

    private fun createQueryPlan(
        selectionSet: GJSelectionSet,
        parentType: GraphQLCompositeType,
    ): QueryPlan {
        check(!built) { "Builder cannot be reused" }
        built = true

        val state = buildState(
            selectionSet,
            State(
                selectionSet = QueryPlan.SelectionSet.empty,
                parentType = parentType,
                constraints = Constraints.Unconstrained,
            )
        )
        return QueryPlan(
            selectionSet = state.selectionSet,
            fragments = QueryPlan.Fragments(fragments.toMap()),
            variablesResolvers = variablesResolvers,
            parentType = parentType
        )
    }

    private fun buildState(
        selectionSet: GJSelectionSet,
        state: State,
    ): State =
        with(state) {
            selectionSet.selections
                .fold(state) { acc, sel ->
                    when (sel) {
                        is GJField -> processField(sel, acc)
                        is GJInlineFragment -> processInlineFragment(sel, acc)
                        is GJFragmentSpread -> processFragmentSpread(sel, acc)
                        else -> throw IllegalStateException("Unexpected selection type: ${sel.javaClass}")
                    }
                }
        }

    private fun buildChildPlans(
        parentType: GraphQLCompositeType,
        field: GJField
    ): List<QueryPlan> {
        val requiredSelectionSets = parameters.registry.getRequiredSelectionSets(parentType.name, field.name, parameters.executeAccessChecksInModstrat)
        if (requiredSelectionSets.isEmpty()) {
            return emptyList()
        }
        return requiredSelectionSets.map { rss ->
            // Use the typeName from ParsedSelections to determine correct target type
            val targetType = parameters.schema.schema.getType(rss.selections.typeName) as GraphQLCompositeType
            QueryPlanBuilder(parameters, rss.selections.fragmentMap, rss.variablesResolvers)
                .createQueryPlan(
                    rss.selections.selections,
                    targetType,
                )
        }
    }

    private fun processField(
        sel: GJField,
        state: State
    ): State =
        with(state) {
            val coord = (state.parentType.name to sel.name).gj
            val fieldDef = parameters.schema.schema.getFieldDefinition(coord)
            val fieldType = GraphQLTypeUtil.unwrapAll(fieldDef.type) as GraphQLNamedOutputType

            val possibleParentTypes = parameters.schema.rels.possibleObjectTypes(parentType)

            val childPlans = buildChildPlans(parentType, sel)

            val fieldConstraints = constraints
                .withDirectives(sel.directives)
                .narrowTypes(possibleParentTypes)

            if (fieldConstraints.solve(Constraints.Ctx.empty) == Constraints.Resolution.Drop) {
                return state
            }

            val subSelectionState = sel.selectionSet?.let { ss ->
                fieldType as GraphQLCompositeType
                val possibleFieldTypes = parameters.schema.rels.possibleObjectTypes(fieldType)
                val subSelectionConstraints = Constraints.Companion(
                    sel.directives,
                    possibleFieldTypes
                )
                buildState(
                    ss,
                    state.copy(
                        selectionSet = QueryPlan.SelectionSet.empty,
                        parentType = fieldType,
                        constraints = subSelectionConstraints,
                    )
                )
            }

            val field = Field(
                resultKey = sel.resultKey,
                constraints = fieldConstraints,
                field = sel,
                selectionSet = subSelectionState?.selectionSet,
                childPlans = childPlans
            )

            state.copy(selectionSet = selectionSet + field)
        }

    private fun processInlineFragment(
        sel: GJInlineFragment,
        state: State
    ): State =
        with(state) {
            val typeConditionName = sel.typeCondition?.name ?: state.parentType.name
            val typeCondition = checkNotNull(parameters.schema.schema.getTypeAs<GraphQLCompositeType>(typeConditionName)) {
                "Type $typeConditionName not found"
            }
            val newConstraints = constraints
                .withDirectives(sel.directives)
                .narrowTypes(
                    parameters.schema.rels.possibleObjectTypes(typeCondition)
                )

            if (newConstraints.solve(Constraints.Ctx.empty) == Constraints.Resolution.Drop) {
                return state
            }

            val fragmentResult = processFragment(
                sel.selectionSet,
                typeConditionName,
                state.copy(
                    selectionSet = QueryPlan.SelectionSet.empty,
                    constraints = newConstraints
                )
            )

            val inlineFragment = QueryPlan.InlineFragment(fragmentResult.selectionSet, newConstraints)
            copy(selectionSet = selectionSet + inlineFragment)
        }

    private fun processFragmentSpread(
        sel: GJFragmentSpread,
        state: State
    ): State =
        with(state) {
            val name = sel.name
            val gjdef = checkNotNull(fragmentsByName[name]) { "Missing fragment definition: $name" }
            val fragType = parameters.schema.schema.getTypeAs<GraphQLCompositeType>(gjdef.typeCondition.name)

            if (name !in fragments) {
                val fragState = buildState(
                    gjdef.selectionSet,
                    State(
                        selectionSet = QueryPlan.SelectionSet.empty,
                        parentType = fragType,
                        constraints = Constraints.Unconstrained,
                    )
                )
                fragments[name] = QueryPlan.FragmentDefinition(fragState.selectionSet)
            }

            val newConstraints = constraints
                .withDirectives(sel.directives)
                .narrowTypes(
                    parameters.schema.rels.possibleObjectTypes(fragType)
                )

            if (newConstraints.solve(Constraints.Ctx.empty) == Constraints.Resolution.Drop) {
                return state
            }

            copy(
                selectionSet = selectionSet + QueryPlan.FragmentSpread(name, newConstraints),
            )
        }

    private fun processFragment(
        gjSelectionSet: GJSelectionSet,
        typeConditionName: String,
        state: State
    ): State {
        val typeCondition = checkNotNull(parameters.schema.schema.getType(typeConditionName) as? GraphQLCompositeType) {
            "Type $typeConditionName not found in schema."
        }

        val newConstraints = state.constraints.narrowTypes(
            parameters.schema.rels.possibleObjectTypes(typeCondition)
        )

        // Check if this fragment combination is impossible
        if (newConstraints.solve(Constraints.Ctx.empty) == Constraints.Resolution.Drop) {
            return state
        }

        return buildState(
            gjSelectionSet,
            state.copy(
                constraints = newConstraints,
                parentType = typeCondition,
            )
        )
    }
}

/** A pointer into a QueryPlan-able element of a GraphQL document */
sealed class DocumentKey {
    /** A pointer to a Fragment definition in a GraphQL document */
    data class Fragment(val name: String) : DocumentKey() {
        init {
            require(name.isNotEmpty()) { "Fragment name may not be an empty string" }
        }
    }

    /** A pointer to an Operation definition in a GraphQL document */
    data class Operation(val name: String?) : DocumentKey() {
        init {
            require(name == null || name.isNotEmpty()) { "Operation name may not be an empty string" }
        }
    }
}
