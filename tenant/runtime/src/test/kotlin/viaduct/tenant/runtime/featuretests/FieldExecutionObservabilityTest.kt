package viaduct.tenant.runtime.featuretests

import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.SimpleInstrumentationContext
import graphql.execution.instrumentation.parameters.InstrumentationFieldParameters
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import viaduct.api.globalid.GlobalID
import viaduct.engine.api.Coordinate
import viaduct.engine.api.FromArgumentVariable
import viaduct.engine.api.FromQueryFieldVariable
import viaduct.engine.api.instrumentation.IViaductInstrumentation
import viaduct.engine.api.instrumentation.ViaductInstrumentationBase
import viaduct.engine.api.observability.ExecutionObservabilityContext
import viaduct.engine.runtime.getLocalContextForType
import viaduct.tenant.runtime.featuretests.fixtures.Baz
import viaduct.tenant.runtime.featuretests.fixtures.FeatureTestBuilder
import viaduct.tenant.runtime.featuretests.fixtures.FeatureTestSchemaFixture
import viaduct.tenant.runtime.featuretests.fixtures.Query
import viaduct.tenant.runtime.featuretests.fixtures.UntypedFieldContext
import viaduct.tenant.runtime.featuretests.fixtures.assertJson

@ExperimentalCoroutinesApi
class FieldExecutionObservabilityTest {
    @Test
    fun `test no operation name should not break`() {
        val instrumentation = FieldRequiredByEntityInstrumentation()

        FeatureTestBuilder()
            .grtPackage(Query.Reflection)
            .sdl(FeatureTestSchemaFixture.sdl)
            .resolver("Query" to "idField", resolverName = "query-id-field-resolver") { ctx ->
                // resolver returns a GRT value to the tenant runtime, which we expect to be unwrapped before
                // it's handed over to the engine
                ctx.globalIDFor(Baz.Reflection, "1")
            }
            .resolver(
                "Query" to "string1",
                objectValueFragment = "idField",
                resolveFn = { ctx: UntypedFieldContext ->
                    // using an UntypedFieldContext, peek at the engine data for the field to ensure that it's
                    // been unwrapped
                    val idFieldValue = ctx.objectValue.get<GlobalID<Baz>>("idField")
                    assertEquals(Baz.Reflection, idFieldValue.type)
                    assertEquals("1", idFieldValue.internalID)
                    idFieldValue.internalID
                },
                resolverName = "query-string1-resolver"
            )
            .instrumentation(instrumentation.asStandardInstrumentation())
            .build()
            .execute("{string1}")
            .assertJson("{data: {string1: \"1\"}}")

        assertEquals(
            setOf(
                ("Query" to "idField"),
                ("Query" to "string1"),
            ),
            instrumentation.fieldToRequiredByLookup.keys
        )

        assertEquals(setOf("RESOLVER:query-string1-resolver"), instrumentation.getFieldRequiredBy("Query", "idField").toSet())
        assertEquals(setOf(null), instrumentation.getFieldRequiredBy("Query", "string1").toSet())
    }

    @Test
    fun `resolver name is passed to instrumentation`() {
        val instrumentation = FieldRequiredByEntityInstrumentation()

        FeatureTestBuilder()
            .grtPackage(Query.Reflection)
            .sdl(FeatureTestSchemaFixture.sdl)
            .resolver("Query" to "idField", resolverName = "query-id-field-resolver") { ctx ->
                // resolver returns a GRT value to the tenant runtime, which we expect to be unwrapped before
                // it's handed over to the engine
                ctx.globalIDFor(Baz.Reflection, "1")
            }
            .resolver(
                "Query" to "string1",
                objectValueFragment = "idField",
                resolveFn = { ctx: UntypedFieldContext ->
                    // using an UntypedFieldContext, peek at the engine data for the field to ensure that it's
                    // been unwrapped
                    val idFieldValue = ctx.objectValue.get<GlobalID<Baz>>("idField")
                    assertEquals(Baz.Reflection, idFieldValue.type)
                    assertEquals("1", idFieldValue.internalID)
                    idFieldValue.internalID
                },
                resolverName = "query-string1-resolver"
            )
            .instrumentation(instrumentation.asStandardInstrumentation())
            .build()
            .execute("query testQuery {string1}")
            .assertJson("{data: {string1: \"1\"}}")

        assertEquals(
            setOf(
                ("Query" to "idField"),
                ("Query" to "string1"),
            ),
            instrumentation.fieldToRequiredByLookup.keys
        )

        assertEquals(setOf("RESOLVER:query-string1-resolver"), instrumentation.getFieldRequiredBy("Query", "idField").toSet())
        assertEquals(setOf("OPERATION:testQuery"), instrumentation.getFieldRequiredBy("Query", "string1").toSet())
    }

    @Test
    fun `test same field is fetched multiple times`() {
        val instrumentation = FieldRequiredByEntityInstrumentation()

        FeatureTestBuilder()
            .grtPackage(Query.Reflection)
            .sdl(FeatureTestSchemaFixture.sdl)
            .resolver("Query" to "idField", resolverName = "query-id-field-resolver") { ctx ->
                // resolver returns a GRT value to the tenant runtime, which we expect to be unwrapped before
                // it's handed over to the engine
                ctx.globalIDFor(Baz.Reflection, "1")
            }
            .resolver(
                "Query" to "string1",
                objectValueFragment = "idField",
                resolveFn = { ctx: UntypedFieldContext ->
                    // using an UntypedFieldContext, peek at the engine data for the field to ensure that it's
                    // been unwrapped
                    val idFieldValue = ctx.objectValue.get<GlobalID<Baz>>("idField")
                    assertEquals(Baz.Reflection, idFieldValue.type)
                    assertEquals("1", idFieldValue.internalID)
                    idFieldValue.internalID
                },
                resolverName = "query-string1-resolver"
            )
            .instrumentation(instrumentation.asStandardInstrumentation())
            .build()
            .execute("query testQuery {first: string1, second: string1}")
            .assertJson("{data: {first: \"1\", second: \"1\"}}")

        assertEquals(
            setOf(
                ("Query" to "idField"),
                ("Query" to "string1"),
            ),
            instrumentation.fieldToRequiredByLookup.keys
        )

        assertTrue(instrumentation.getFieldRequiredBy("Query", "idField")?.toSet()?.contains("RESOLVER:query-string1-resolver") ?: false) {
            "Expected 'query-string1-resolver' to be in the required by set for 'idField'"
        }
        assertTrue((instrumentation.getFieldRequiredBy("Query", "idField")?.size == 2)) {
            "Expected 'idField' to be required by the resolver twice, but found: ${instrumentation.getFieldRequiredBy("Query", "idField")}"
        }
    }

    @Test
    fun `test a field is queried by both the operation and the resolver`() {
        val instrumentation = FieldRequiredByEntityInstrumentation()

        FeatureTestBuilder()
            .grtPackage(Query.Reflection)
            .sdl(FeatureTestSchemaFixture.sdl)
            .resolver("Query" to "idField", resolverName = "query-id-field-resolver") { ctx ->
                // resolver returns a GRT value to the tenant runtime, which we expect to be unwrapped before
                // it's handed over to the engine
                ctx.globalIDFor(Baz.Reflection, "1")
            }
            .resolver(
                "Query" to "string1",
                objectValueFragment = "idField",
                resolveFn = { ctx: UntypedFieldContext ->
                    // using an UntypedFieldContext, peek at the engine data for the field to ensure that it's
                    // been unwrapped
                    val idFieldValue = ctx.objectValue.get<GlobalID<Baz>>("idField")
                    assertEquals(Baz.Reflection, idFieldValue.type)
                    assertEquals("1", idFieldValue.internalID)
                    idFieldValue.internalID
                },
                resolverName = "query-string1-resolver"
            )
            .instrumentation(instrumentation.asStandardInstrumentation())
            .build()
            .execute("query testQuery {idField, string1}")
        // No need to verify the execution result.

        assertEquals(
            setOf(
                ("Query" to "idField"),
                ("Query" to "string1"),
            ),
            instrumentation.fieldToRequiredByLookup.keys
        )

        assertEquals(setOf("RESOLVER:query-string1-resolver", "OPERATION:testQuery"), instrumentation.getFieldRequiredBy("Query", "idField").toSet())
        assertEquals(setOf("OPERATION:testQuery"), instrumentation.getFieldRequiredBy("Query", "string1")?.toSet())
    }

    @Test
    fun `test a field is queried by multiple resolvers`() {
        val instrumentation = FieldRequiredByEntityInstrumentation()

        FeatureTestBuilder()
            .grtPackage(Query.Reflection)
            .sdl(FeatureTestSchemaFixture.sdl)
            .resolver("Query" to "idField", resolverName = "query-id-field-resolver") { ctx ->
                // resolver returns a GRT value to the tenant runtime, which we expect to be unwrapped before
                // it's handed over to the engine
                ctx.globalIDFor(Baz.Reflection, "1")
            }
            .resolver(
                "Query" to "string1",
                objectValueFragment = "idField",
                resolveFn = { ctx: UntypedFieldContext ->
                    val idFieldValue = ctx.objectValue.get<GlobalID<Baz>>("idField")
                    idFieldValue.internalID
                },
                resolverName = "query-string1-resolver"
            )
            .resolver(
                "Query" to "string2",
                objectValueFragment = "idField",
                resolveFn = { ctx: UntypedFieldContext ->
                    val idFieldValue = ctx.objectValue.get<GlobalID<Baz>>("idField")
                    idFieldValue.internalID
                },
                resolverName = "query-string2-resolver"
            )
            .instrumentation(instrumentation.asStandardInstrumentation())
            .build()
            .execute("query testQuery {string1, string2}")
            .assertJson("{data: {string1: \"1\", string2: \"1\"}}")

        assertEquals(
            setOf(
                ("Query" to "idField"),
                ("Query" to "string1"),
                ("Query" to "string2")
            ),
            instrumentation.fieldToRequiredByLookup.keys
        )

        assertEquals(setOf("RESOLVER:query-string1-resolver", "RESOLVER:query-string2-resolver"), instrumentation.getFieldRequiredBy("Query", "idField").toSet())
    }

    @Test
    fun `test a field is queried by multiple resolvers with different args`() {
        val instrumentation = FieldRequiredByEntityInstrumentation()

        FeatureTestBuilder()
            .grtPackage(Query.Reflection)
            .sdl(FeatureTestSchemaFixture.sdl)
            .resolver(
                "Query" to "hasArgs2",
                resolveFn = { it.arguments.get<String>("x") + "_resolved" },
                resolverName = "query-has-args1-resolver"
            )
            .resolver(
                "Query" to "string1",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<String>("hasArgs2") },
                "hasArgs2(x:\"string1\")",
                resolverName = "query-string1-resolver"
            )
            .resolver(
                "Query" to "string2",
                { ctx: UntypedFieldContext -> ctx.objectValue.get<String>("hasArgs2") },
                "hasArgs2(x:\"string2\")",
                resolverName = "query-string2-resolver"
            )
            .instrumentation(instrumentation.asStandardInstrumentation())
            .build()
            .execute("query testQuery {string1, string2}")
            .assertJson("{data: {string1: \"string1_resolved\", string2: \"string2_resolved\"}}")

        assertEquals(
            setOf(
                ("Query" to "hasArgs2"),
                ("Query" to "string1"),
                ("Query" to "string2"),
            ),
            instrumentation.fieldToRequiredByLookup.keys
        )

        assertEquals(setOf("RESOLVER:query-string1-resolver", "RESOLVER:query-string2-resolver"), instrumentation.getFieldRequiredBy("Query", "hasArgs2").toSet())
    }

    @Test
    fun `test a field is queried by same resolver multiple times`() {
        val instrumentation = FieldRequiredByEntityInstrumentation()

        FeatureTestBuilder()
            .grtPackage(Query.Reflection)
            .sdl(FeatureTestSchemaFixture.sdl)
            .resolver(
                "Query" to "hasArgs1",
                resolveFn = { it.arguments.get<Int>("x") * 2 },
                resolverName = "query-has-args1-resolver"
            )
            .resolver(
                "Query" to "hasArgs3",
                resolveFn = { ctx: UntypedFieldContext -> ctx.objectValue.get<Int>("hasArgs1") * 3 },
                "hasArgs1(x:\$x)",
                variables = listOf(FromArgumentVariable("x", "x")),
                resolverName = "query-has-args3-resolver"
            )
            .instrumentation(instrumentation.asStandardInstrumentation())
            .build()
            .execute("query testQuery {first: hasArgs3(x: 1), second: hasArgs3(x: 2)}")
            .assertJson("{data: {first: 6, second: 12}}")

        assertEquals(
            setOf(
                ("Query" to "hasArgs1"),
                ("Query" to "hasArgs3"),
            ),
            instrumentation.fieldToRequiredByLookup.keys
        )

        assertTrue(instrumentation.getFieldRequiredBy("Query", "hasArgs1").toSet().contains("RESOLVER:query-has-args3-resolver")) {
            "Expected 'query-has-args3-resolver' to be in the required by set for 'hasArgs1'"
        }
        assertTrue(instrumentation.getFieldRequiredBy("Query", "hasArgs1").size == 2) {
            "Expected 'hasArgs1' to be required by the same resolver twice, but found: ${instrumentation.getFieldRequiredBy("Query", "hasArgs1")}"
        }
    }

    @Test
    fun `test variable resolvers`() {
        val instrumentation = FieldRequiredByEntityInstrumentation()

        FeatureTestBuilder()
            .sdl("type Query { x:Int, y(b:Int):Int, z:Int }")
            .resolver(
                "Query" to "x",
                { ctx: UntypedFieldContext -> ctx.queryValue.get<Int>("y") * 5 },
                queryValueFragment = "y(b:\$b), z",
                variables = listOf(FromQueryFieldVariable("b", "z")),
                resolverName = "query-x-resolver"
            )
            .resolver("Query" to "y", resolverName = "query-y-resolver") { it.arguments.get<Int>("b") * 3 }
            .resolver("Query" to "z", resolverName = "query-z-resolver") { 2 }
            .instrumentation(instrumentation.asStandardInstrumentation())
            .build()
            .assertJson("{data: {x: 30}}", "query testQuery{x}")

        assertEquals(
            setOf(
                ("Query" to "x"),
                ("Query" to "y"),
                ("Query" to "z"),
            ),
            instrumentation.fieldToRequiredByLookup.keys
        )

        assertTrue(instrumentation.getFieldRequiredBy("Query", "x").toSet().contains("OPERATION:testQuery")) {
            "Expected 'OPERATION:testQuery' to be in the required by set for 'Query.x' but found: ${instrumentation.getFieldRequiredBy("Query", "x")}"
        }
        assertTrue(instrumentation.getFieldRequiredBy("Query", "y").toSet().contains("RESOLVER:query-x-resolver")) {
            "Expected 'RESOLVER:query-x-resolver' to be in the required by set for 'Query.y' but found: ${instrumentation.getFieldRequiredBy("Query", "y")}"
        }
        assertTrue(instrumentation.getFieldRequiredBy("Query", "z").toSet().contains("VARIABLES_RESOLVER:RESOLVER:query-x-resolver")) {
            "Expected 'VARIABLE_RESOLVER:RESOLVER:query-x-resolver' to be in the required by set for 'Query.z' but found: ${instrumentation.getFieldRequiredBy("Query", "z")}"
        }
    }

    class FieldRequiredByEntityInstrumentation : ViaductInstrumentationBase(), IViaductInstrumentation.WithBeginFieldExecution {
        val fieldToRequiredByLookup = ConcurrentHashMap<Coordinate, CopyOnWriteArrayList<String?>>()

        override fun beginFieldExecution(
            parameters: InstrumentationFieldParameters,
            state: InstrumentationState?
        ): InstrumentationContext<Any>? {
            val typeName = parameters.executionStepInfo.objectType.name
            val fieldName = parameters.executionStepInfo.field.name
            val coordinate = (typeName to fieldName)

            val attribution = parameters.executionContext.getLocalContextForType<ExecutionObservabilityContext>()?.attribution
            fieldToRequiredByLookup.computeIfAbsent(coordinate) { CopyOnWriteArrayList() }
                .add(attribution?.toTagString())

            return SimpleInstrumentationContext.noOp()
        }

        fun getFieldRequiredBy(
            typeName: String,
            fieldName: String
        ): List<String?> {
            return fieldToRequiredByLookup[(typeName to fieldName)] ?: emptyList()
        }
    }
}
