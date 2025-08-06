@file:OptIn(ExperimentalCoroutinesApi::class)

package viaduct.engine.runtime.execution

import graphql.GraphQLContext
import graphql.introspection.Introspection as GJIntrospection
import graphql.introspection.IntrospectionQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IntrospectionConformanceTest {
    private val sdl: String = """
        directive @dir1 on FIELD_DEFINITION

        type Object1 implements Interface1 {
            f1: Int!
            f2: Int @deprecated(reason: "reason")
            f3: Int! @deprecated(reason: "reason")
            f4: Object1!
            f5(x: Int, y: Int = 2, z: Int @deprecated): Int
            f6: Int @dir1
        }

        type Object2 { f1: String! }

        input Input1 {
            x: Int!
            y: Int = 2
            z: Int! = 2 @deprecated(reason: "reason")
            input1: Input1
        }

        input Input2 @oneOf { x: Int y: Int }

        enum Enum1 { V1, V2, V3 @deprecated }

        union Union1 = Object1 | Object2
        interface Interface1 { f1: Int! }

        interface Interface2 implements Interface1 { f1: Int! f2: Int @deprecated(reason: "reason") }

        type Query { greeting: String }
    """.trimIndent()

    private fun mkContext(introspectionDisabled: Boolean): GraphQLContext =
        GraphQLContext.newContext()
            .put(GJIntrospection.INTROSPECTION_DISABLED, introspectionDisabled)
            .build()

    private val disabledCtx = mkContext(introspectionDisabled = true)
    private val enabledCtx = mkContext(introspectionDisabled = false)

    private fun Conformer.check(
        query: String,
        ctx: GraphQLContext,
        checkResult: CheckResult
    ) {
        check(query, checkNoModernErrors = false, graphQLContext = ctx, extraChecks = checkResult)
    }

    private val assertRejected = CheckResult { _, (act) ->
        assertNull(act.getData<Map<String, Any?>>())
        assertEquals(1, act.errors.size)
        assertEquals("IntrospectionDisabled", act.errors.first().errorType.toString())
    }

    private val assertAccepted = CheckResult { _, (act) ->
        assertNotNull(act.getData<Map<String, Any?>>())
        assertTrue(act.errors.isEmpty())
    }

    @Test
    fun `introspection enabled -- no flag set in GraphQLContext`() {
        Conformer(sdl)
            .check(
                IntrospectionQuery.INTROSPECTION_QUERY,
                GraphQLContext.getDefault(),
                assertAccepted
            )
    }

    @Test
    fun `introspection enabled -- enabled in GraphQLContext`() {
        Conformer(sdl)
            .check(IntrospectionQuery.INTROSPECTION_QUERY, enabledCtx, assertAccepted)
    }

    @Test
    fun `introspection disabled -- disabled in GraphQLContext`() {
        Conformer(sdl)
            .check(IntrospectionQuery.INTROSPECTION_QUERY, disabledCtx, assertRejected)
    }

    @Test
    fun `introspection disabled -- non-introspection query`() {
        Conformer(sdl).check("{ greeting }", disabledCtx, assertAccepted)
    }

    @Test
    fun `introspection disabled -- simple __type selection`() {
        Conformer(sdl).check("{ __type(name:\"Query\") { name } }", disabledCtx, assertRejected)
    }

    @Test
    fun `introspection disabled -- aliased __type selection`() {
        Conformer(sdl).check("{ alias:__type(name:\"Query\") { name } }", disabledCtx, assertRejected)
    }

    @Test
    fun `introspection disabled -- simple __schema selection`() {
        Conformer(sdl).check("{ __schema { __typename }}", disabledCtx, assertRejected)
    }

    @Test
    fun `introspection disabled -- aliased __schema selection`() {
        Conformer(sdl).check("{ alias:__schema { __typename }}", disabledCtx, assertRejected)
    }

    @Test
    fun `introspection disabled -- simple __typename selection are allowed`() {
        // a __typename field selections is innocuous and allowed even when introspection is disabled
        Conformer(sdl).check("{ __typename }", disabledCtx, assertAccepted)
    }

    @Test
    fun `introspection disabled -- aliased __typename selection`() {
        // a __typename field selections is innocuous and allowed even when introspection is disabled
        Conformer(sdl).check("{ alias: __typename }", disabledCtx, assertAccepted)
    }

    @Test
    fun `sanity -- no unknown introspection fields`() {
        // Our support for introspection uses GJ introspection for execution, but depends on a
        // viaduct-specific layer for validating that a query does not use any known introspection fields.
        // This only works if the viaduct validation layer has an accurate view of all executable introspection fields,
        // which can change over time as the GraphQL spec evolves.
        // As a sanity check, check that all executable introspection fields are in the list of fields that we know
        // how to validate.
        assertEquals(
            Introspection.disallowedIntrospectionFields + Introspection.allowedIntrospectionFields,
            GJIntrospection.INTROSPECTION_SYSTEM_FIELDS
        )
    }
}
