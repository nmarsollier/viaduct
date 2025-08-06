package viaduct.graphql.schema.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.graphql.schema.ViaductSchema

/** A set of unit tests for SimpleBridgeSchema implementations, with emphasis on
 *  catching error cases (non-error cases are tested pretty well in the "full"
 *  tests).
 */
interface ViaductSchemaContract {
    companion object {
        private fun ViaductSchema.withType(
            type: String,
            block: (ViaductSchema.TypeDef) -> Unit
        ) = block(this.types[type] ?: throw IllegalArgumentException("Unknown type $type"))

        private fun ViaductSchema.withField(
            type: String,
            field: String,
            block: (ViaductSchema.Field) -> Unit
        ) = block((this.types[type]!! as ViaductSchema.Record).field(field)!!)

        private fun ViaductSchema.withFieldArg(
            type: String,
            field: String,
            arg: String,
            block: (ViaductSchema.FieldArg) -> Unit
        ) = block((this.types[type]!! as ViaductSchema.Record).field(field)!!.args.find { it.name == arg }!!)

        private fun ViaductSchema.withEnumValue(
            enum: String,
            value: String,
            block: (ViaductSchema.EnumValue) -> Unit
        ) = block((this.types[enum]!! as ViaductSchema.Enum).value(value)!!)

        private fun assertToStingContains(
            msg: String,
            def: ViaductSchema.Def,
            vararg expected: String
        ) {
            val actual = def.toString()
            for (e in expected) {
                assertTrue(actual.contains(e), "$msg: $actual")
            }
            assertTrue(actual.contains(def.name), "$msg: $actual")
        }
    }

    fun makeSchema(schema: String): ViaductSchema

    @Test
    fun `Effective default funs should throw on fields of output types`() {
        makeSchema(
            """
            type Query {
                foo: String
            }
            interface F {
                foo: String
            }
            """.trimIndent()
        ).apply {
            withField("Query", "foo") {
                assertFalse(it.hasEffectiveDefault, "Query")
                assertThrows<NSE>("Query") { it.effectiveDefaultValue }
            }
            withField("F", "foo") {
                assertFalse(it.hasEffectiveDefault, "F")
                assertThrows<NSE>("F") { it.effectiveDefaultValue }
            }
        }
    }

    @Test
    fun `Effective default funs should throw on has-defaults with no default`() {
        makeSchema(
            """
            type Query {
                foo(bar: String!): String
            }
            input I {
                foo: String!
            }
            """.trimIndent()
        ).apply {
            withFieldArg("Query", "foo", "bar") {
                assertFalse(it.hasEffectiveDefault, "Query")
                assertThrows<NSE>("Query") { it.effectiveDefaultValue }
            }
            withField("I", "foo") {
                assertFalse(it.hasEffectiveDefault, "I")
                assertThrows<NSE>("I") { it.effectiveDefaultValue }
            }
        }
    }

    @Test
    fun `Effective default funs shouldn't throw on has-defaults not from output types`() {
        makeSchema(
            """
            type Query {
                foo(a: String, b: Int! = 1): String
            }
            input I {
                a: String
                b: Int! = 1
            }
            """.trimIndent()
        ).apply {
            withFieldArg("Query", "foo", "a") {
                assertTrue(it.hasEffectiveDefault, "Query.a")
                assertNull(it.effectiveDefaultValue, "Query.a")
            }
            withFieldArg("Query", "foo", "b") {
                assertTrue(it.hasEffectiveDefault, "Query.b")
                assertNotNull(it.effectiveDefaultValue, "Query.b")
            }
            withField("I", "a") {
                assertTrue(it.hasEffectiveDefault, "I.a")
                assertNull(it.effectiveDefaultValue, "I.a")
            }
            withField("I", "b") {
                assertTrue(it.hasEffectiveDefault, "I.b")
                assertNotNull(it.effectiveDefaultValue, "I.b")
            }
        }
    }

    @Test
    fun `Test the fields getter that takes a path`() {
        makeSchema(
            """
            type Query {
                a: A
            }
            interface A {
                b: B
            }
            interface B {
                c: C
            }
            type C {
                d: String
            }
            """.trimIndent()
        ).apply {
            val query = this.types["Query"]!! as ViaductSchema.Object
            assertThrows<IllegalArgumentException>("empty") { query.field(listOf()) }
            assertThrows<IllegalArgumentException>("missing") { query.field(listOf("foo")) }
            assertThrows<IllegalArgumentException>("scalar") { query.field(listOf("a", "b", "c", "d", "e")) }
            withField("A", "b") { assertSame(it, query.field(listOf("a", "b"))) }
            withField("B", "c") { assertSame(it, query.field(listOf("a", "b", "c"))) }
            withField("C", "d") { assertSame(it, query.field(listOf("a", "b", "c", "d"))) }
        }
    }

    @Test
    fun `isOverride is computed correctly`() {
        makeSchema(
            """
            type Query { foo: String }
            interface A { a: String }
            interface B implements A { a: String! b: String }
            interface C implements A&B { a: String! b: String c: Int }
            """.trimIndent()
        ).apply {
            withField("A", "a") {
                assertFalse(it.isOverride, "A.a")
            }
            withField("B", "a") {
                assertTrue(it.isOverride, "B.a")
            }
            withField("B", "b") {
                assertFalse(it.isOverride, "B.b")
            }
            withField("C", "a") {
                assertTrue(it.isOverride, "C.a")
            }
            withField("C", "b") {
                assertTrue(it.isOverride, "C.b")
            }
            withField("C", "c") {
                assertFalse(it.isOverride, "C.c")
            }
        }
    }

    @Test
    fun `Descriptions are descriptive`() {
        makeSchema(
            """
            type Query { foo(a: Int): String }
            enum E { A }
            input I { a: Int }
            interface A { a(b: [String]): Int }
            scalar S
            union U = Query
            """.trimIndent()
        ).apply {
            withType("Query") { assertToStingContains("Query", it, "Object") }
            withField("Query", "foo") { assertToStingContains("Query.foo", it, "Field", "String") }
            withFieldArg("Query", "foo", "a") { assertToStingContains("Query.foo.a", it, "Arg", "Int") }
            withType("E") { assertToStingContains("E", it, "Enum") }
            (this.types["E"]!! as ViaductSchema.Enum).value("A")!!.let { assertToStingContains("E.A", it, "EnumValue") }
            withType("I") { assertToStingContains("I", it, "Input") }
            withField("I", "a") { assertToStingContains("I.a", it, "Field", "Int") }
            withType("A") { assertToStingContains("A", it, "Interface") }
            withField("A", "a") { assertToStingContains("A.a", it, "Field", "Int") }
            withFieldArg("A", "a", "b") { assertToStingContains("I.a.b", it, "Arg", "String") }
            withType("S") { assertToStingContains("S", it, "Scalar") }
            withType("U") { assertToStingContains("U", it, "Union") }
        }
    }

    @Test
    fun `oneOf directive application`() {
        fun mkSchema(sdl: String) =
            makeSchema(
                """
                schema { query: Query }
                type Query { placeholder: Int }
                $sdl
                """.trimIndent()
            )

        // simple
        mkSchema("input Input @oneOf { a: Int }")
            .also {
                assertTrue(it.types["Input"]!!.hasAppliedDirective("oneOf"))
            }

        // nested
        mkSchema(
            """
            input Outer @oneOf { a: Inner }
            input Inner @oneOf { a: Int }
            """.trimIndent()
        ).also { schema ->
            listOf("Outer", "Inner").forEach {
                schema.withType(it) {
                    assertTrue(it.hasAppliedDirective("oneOf"), it.name)
                }
            }
        }

        // recursive
        mkSchema("input Input @oneOf { a: Input }")
            .also {
                it.withType("Input") {
                    assertTrue(it.hasAppliedDirective("oneOf"), "Input")
                }
            }
    }

    @Test
    fun `test appliedDirectives returns the right list of directive names`() {
        // TODO(https://app.asana.com/1/150975571430/project/1203659453427089/task/1210815630416759?focus=true): add tests for directives on args & scalars
        makeSchema(
            """
            directive @d1 on OBJECT | INPUT_OBJECT | ENUM | INTERFACE | UNION
            directive @d2 on OBJECT | INPUT_OBJECT | ENUM | INTERFACE | UNION
            directive @d3 on FIELD_DEFINITION | INPUT_FIELD_DEFINITION | ENUM_VALUE
            directive @d4 on FIELD_DEFINITION | INPUT_FIELD_DEFINITION | ENUM_VALUE
            type Query @d1 {
                f1: String @d3
            }
            extend type Query @d2 {
                f2: Int @d3 @d4
            }
            enum Enum @d1 {
                V1 @d3
            }
            extend enum Enum @d2 {
                V2 @d3 @d4
            }
            input Input @d1 {
                f1: Boolean @d3
            }
            extend input Input @d2 {
                f2: Float @d3 @d4
            }
            interface Interface @d1 {
                f1: Enum @d3
            }
            extend interface Interface @d2 {
                f2: String @d3 @d4
            }
            type Object {
                f1: String
            }
            union Union @d1 = Query
            extend union Union @d2 = Object
            """.trimIndent()
        ).apply {
            listOf("Query", "Enum", "Input", "Interface", "Union").forEach {
                withType(it) {
                    assertEquals(listOf("d1", "d2"), it.appliedDirectives.map { it.name })
                }
            }
            withField("Query", "f1") {
                assertEquals(listOf("d3"), it.appliedDirectives.map { it.name })
            }
            withField("Query", "f2") {
                assertEquals(listOf("d3", "d4"), it.appliedDirectives.map { it.name })
            }
            withEnumValue("Enum", "V1") {
                assertEquals(listOf("d3"), it.appliedDirectives.map { it.name })
            }
            withEnumValue("Enum", "V2") {
                assertEquals(listOf("d3", "d4"), it.appliedDirectives.map { it.name })
            }
            withField("Input", "f1") {
                assertEquals(listOf("d3"), it.appliedDirectives.map { it.name })
            }
            withField("Input", "f2") {
                assertEquals(listOf("d3", "d4"), it.appliedDirectives.map { it.name })
            }
            withField("Interface", "f1") {
                assertEquals(listOf("d3"), it.appliedDirectives.map { it.name })
            }
            withField("Interface", "f2") {
                assertEquals(listOf("d3", "d4"), it.appliedDirectives.map { it.name })
            }
        }
    }

    @Test
    fun `test root referential integrity`() {
        makeSchema(
            """
            schema {
               query: Foo
               mutation: Bar
               subscription: Baz
            }
            type Foo { blank: String }
            type Bar { blank: String }
            type Baz { blank: String }
            """.trimIndent()
        ).apply {
            assertSame(this.types["Foo"], this.queryTypeDef)
            assertSame(this.types["Bar"], this.mutationTypeDef)
            assertSame(this.types["Baz"], this.subscriptionTypeDef)
        }
    }

    @Test
    fun `test null roots are null`() {
        makeSchema(
            """
            schema {
               query: Query
            }
            type Query { blank: String }
            """.trimIndent()
        ).apply {
            assertNull(this.mutationTypeDef)
            assertNull(this.subscriptionTypeDef)
        }
    }
}
