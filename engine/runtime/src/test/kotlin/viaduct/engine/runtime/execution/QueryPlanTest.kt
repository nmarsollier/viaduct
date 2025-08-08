@file:OptIn(ExperimentalCoroutinesApi::class)

package viaduct.engine.runtime.execution

import graphql.execution.ResultPath
import graphql.language.Argument
import graphql.language.AstPrinter
import graphql.language.Directive as GJDirective
import graphql.language.Field as GJField
import graphql.language.Node
import graphql.language.SelectionSet as GJSelectionSet
import graphql.language.VariableReference
import graphql.schema.GraphQLObjectType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.map
import viaduct.arbitrary.graphql.asDocument
import viaduct.arbitrary.graphql.asSchema
import viaduct.engine.api.RequiredSelectionSetRegistry
import viaduct.engine.api.VariablesResolver
import viaduct.engine.api.ViaductSchema
import viaduct.engine.runtime.execution.ExecutionTestHelpers.executeViaductModernGraphQL
import viaduct.engine.runtime.execution.ExecutionTestHelpers.runExecutionTest
import viaduct.engine.runtime.execution.QueryPlan.CollectedField
import viaduct.engine.runtime.execution.QueryPlan.Field
import viaduct.engine.runtime.execution.QueryPlan.FragmentDefinition
import viaduct.engine.runtime.execution.QueryPlan.FragmentSpread
import viaduct.engine.runtime.execution.QueryPlan.Fragments
import viaduct.engine.runtime.execution.QueryPlan.InlineFragment
import viaduct.engine.runtime.execution.QueryPlan.Selection
import viaduct.engine.runtime.execution.QueryPlan.SelectionSet
import viaduct.service.api.spi.Flags
import viaduct.service.api.spi.mocks.MockFlagManager

class QueryPlanTest {
    @Test
    fun `field`() {
        Fixture("type Query { x:Int }") {
            expectThat(mkPlan("{x}")) {
                checkEquals(
                    QueryPlan(
                        SelectionSet(
                            listOf(
                                Field(
                                    "x",
                                    Constraints(emptyList(), possibleTypes = setOf(query)),
                                    GJField("x"),
                                    null,
                                    emptyList()
                                )
                            )
                        ),
                        Fragments.empty,
                        emptyList(),
                        query
                    )
                )
            }
        }
    }

    @Test
    fun `field -- with directives`() {
        val skipDir = GJDirective.newDirective()
            .name("skip")
            .argument(Argument("if", VariableReference.of("var")))
            .build()

        Fixture("type Query { x:Int }") {
            expectThat(mkPlan("{x @skip(if:\$var) }")) {
                checkEquals(
                    QueryPlan(
                        SelectionSet(
                            listOf(
                                Field(
                                    "x",
                                    Constraints(listOf(skipDir), possibleTypes = setOf(query)),
                                    GJField.newField("x").directive(skipDir).build(),
                                    null,
                                    emptyList()
                                )
                            )
                        ),
                        Fragments.empty,
                        emptyList(),
                        query
                    )
                )
            }
        }
    }

    @Test
    fun `field with subselections`() {
        Fixture("type Query { q:Query }") {
            expectThat(mkPlan("{ q { __typename } }")) {
                checkEquals(
                    QueryPlan(
                        SelectionSet(
                            listOf(
                                Field(
                                    "q",
                                    Constraints(emptyList(), possibleTypes = setOf(query)),
                                    GJField(
                                        "q",
                                        GJSelectionSet(listOf(GJField("__typename")))
                                    ),
                                    SelectionSet(
                                        listOf(
                                            Field(
                                                "__typename",
                                                Constraints(emptyList(), possibleTypes = setOf(query)),
                                                GJField("__typename"),
                                                null,
                                                emptyList()
                                            )
                                        )
                                    ),
                                    emptyList()
                                )
                            )
                        ),
                        Fragments.empty,
                        emptyList(),
                        query
                    )
                )
            }
        }
    }

    @Test
    fun `inline fragment`() {
        Fixture("type Query { x:Int }") {
            expectThat(mkPlan("{ ... { x } }")) {
                checkEquals(
                    QueryPlan(
                        SelectionSet(
                            listOf(
                                InlineFragment(
                                    SelectionSet(
                                        listOf(
                                            Field(
                                                "x",
                                                Constraints(emptyList(), possibleTypes = setOf(query)),
                                                GJField("x"),
                                                null,
                                                emptyList()
                                            )
                                        )
                                    ),
                                    Constraints(emptyList(), possibleTypes = setOf(query)),
                                )
                            )
                        ),
                        Fragments.empty,
                        emptyList(),
                        query
                    )
                )
            }
        }
    }

    @Test
    fun `fragment spread`() {
        Fixture("type Query { x:Int }") {
            val plan = mkPlan(
                """
                    { ... F }
                    fragment F on Query { x }
                """.trimIndent()
            )
            expectThat(plan) {
                checkEquals(
                    QueryPlan(
                        SelectionSet(
                            listOf(
                                FragmentSpread("F", Constraints(emptyList(), possibleTypes = setOf(query))),
                            )
                        ),
                        Fragments(
                            mapOf(
                                "F" to FragmentDefinition(
                                    SelectionSet(
                                        listOf(
                                            Field(
                                                "x",
                                                Constraints(emptyList(), possibleTypes = setOf(query)),
                                                GJField("x"),
                                                null,
                                                emptyList()
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        emptyList(),
                        query
                    )
                )
            }
        }
    }

    @Test
    fun `QueryPlanBuilder -- uses cache by default`() {
        // sanity
        QueryPlan.resetCache()
        assertEquals(0, QueryPlan.cacheSize)
        runExecutionTest {
            executeViaductModernGraphQL("type Query {x:Int}", resolvers = emptyMap(), "{__typename}")
        }
        assertEquals(1, QueryPlan.cacheSize)
    }

    @Test
    fun `QueryPlanBuilder -- bypasses cache when configured`() {
        // sanity
        QueryPlan.resetCache()
        assertEquals(0, QueryPlan.cacheSize)
        runExecutionTest {
            executeViaductModernGraphQL(
                "type Query {x:Int}",
                resolvers = emptyMap(),
                "{__typename}",
                flagManager = MockFlagManager.mk(Flags.DISABLE_QUERY_PLAN_CACHE)
            )
        }
        assertEquals(0, QueryPlan.cacheSize)
    }

    private class Fixture(sdl: String, fn: Fixture.() -> Unit) {
        val schema = sdl.asSchema
        val query: GraphQLObjectType = schema.queryType

        init {
            fn(this)
        }

        fun mkPlan(doc: String): QueryPlan = mkPlan(doc, ViaductSchema(schema))
    }
}

internal fun Assertion.Builder<QueryPlan>.checkEquals(exp: QueryPlan): Assertion.Builder<QueryPlan> =
    and {
        get { selectionSet }.checkEquals(exp.selectionSet)
        get { fragments }.checkEquals(exp.fragments)
        with({ variablesResolvers }) {
            hasSize(exp.variablesResolvers.size)
            exp.variablesResolvers.zip(subject).forEach { (expvr, actvr) ->
                with({ actvr }) {
                    checkEquals(expvr)
                }
            }
        }
    }

internal fun Assertion.Builder<SelectionSet>.checkEquals(exp: SelectionSet): Assertion.Builder<SelectionSet> =
    and {
        with({ selections }) {
            hasSize(exp.selections.size)

            exp.selections.zip(subject).forEach { (expSel, actSel) ->
                with({ actSel }) {
                    checkEquals(expSel)
                }
            }
        }
    }

internal fun Assertion.Builder<Fragments>.checkEquals(exp: Fragments): Assertion.Builder<Fragments> =
    and {
        with({ map }) {
            hasSize(exp.map.size)
            if (exp.map.isNotEmpty()) {
                exp.forEach { (expName, expDef) ->
                    with({ get(expName) }) {
                        isNotNull().checkEquals(expDef)
                    }
                }
            }
        }
    }

internal fun Assertion.Builder<VariablesResolver>.checkEquals(exp: VariablesResolver): Assertion.Builder<VariablesResolver> =
    and {
        isEqualTo(exp)
    }

internal fun <T : Selection> Assertion.Builder<T>.checkEquals(exp: T): Assertion.Builder<T> =
    and {
        when (exp) {
            is Field -> {
                isA<Field>().and {
                    get { resultKey }.isEqualTo(exp.resultKey)
                    get { constraints }.isEqualTo(exp.constraints)
                    get { field }.checkEquals(exp.field)
                    with({ selectionSet }) {
                        exp.selectionSet?.let {
                            isNotNull().checkEquals(it)
                        } ?: isNull()
                    }
                    get { childPlans }.checkEquals(exp.childPlans)
                }
            }

            is FragmentSpread -> {
                isA<FragmentSpread>().and {
                    get { name }.isEqualTo(exp.name)
                    get { constraints }.isEqualTo(exp.constraints)
                }
            }

            is InlineFragment -> {
                isA<InlineFragment>().and {
                    get { selectionSet }.checkEquals(exp.selectionSet)
                    get { constraints }.isEqualTo(exp.constraints)
                }
            }

            is CollectedField -> {
                isA<CollectedField>().and {
                    get { responseKey }.isEqualTo(exp.responseKey)
                    with({ selectionSet }) {
                        exp.selectionSet?.let {
                            isNotNull().checkEquals(it)
                        } ?: isNull()
                    }
                    assertMergedFieldsEqual(ResultPath.rootPath(), exp.mergedField, subject.mergedField)
                    get { childPlans }.checkEquals(exp.childPlans)
                }
            }

            else ->
                assertThat("Unhandled selection type: ${exp::class.simpleName}") { false }
        }
    }

internal fun Assertion.Builder<List<QueryPlan>>.checkEquals(exp: List<QueryPlan>): Assertion.Builder<List<QueryPlan>> =
    and {
        hasSize(exp.size)
        if (exp.isNotEmpty()) {
            with({ exp.zip(subject) }) {
                map { (expCp, actCp) ->
                    with({ actCp }) {
                        checkEquals(expCp)
                    }
                }
            }
        }
    }

internal fun Assertion.Builder<FragmentDefinition>.checkEquals(exp: FragmentDefinition): Assertion.Builder<FragmentDefinition> =
    and {
        get { selectionSet }.checkEquals(exp.selectionSet)
    }

internal fun <T : Node<*>> Assertion.Builder<T>.checkEquals(exp: T): Assertion.Builder<T> =
    and {
        get { AstPrinter.printAst(this) }.isEqualTo(AstPrinter.printAst(exp))
    }

internal fun mkPlan(
    doc: String,
    schema: ViaductSchema,
): QueryPlan =
    runExecutionTest {
        mkQPParameters(doc, schema).let { params ->
            QueryPlan.build(params, doc.asDocument)
        }
    }

internal fun mkQPParameters(
    doc: String,
    schema: ViaductSchema,
): QueryPlan.Parameters =
    QueryPlan.Parameters(
        doc,
        schema,
        RequiredSelectionSetRegistry.Empty,
        // passing false here as it is not relevant for the tests we are running here given empty RSS registry
        executeAccessChecksInModstrat = false
    )
