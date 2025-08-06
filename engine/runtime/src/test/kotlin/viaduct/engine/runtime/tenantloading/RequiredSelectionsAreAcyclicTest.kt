package viaduct.engine.runtime.tenantloading

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import viaduct.engine.api.Coordinate
import viaduct.engine.api.FromObjectFieldVariable
import viaduct.engine.api.FromQueryFieldVariable
import viaduct.engine.api.ParsedSelections
import viaduct.engine.api.VariablesResolver
import viaduct.engine.api.ViaductSchema
import viaduct.engine.api.mocks.MockRequiredSelectionSetRegistry
import viaduct.engine.api.mocks.MockSchema
import viaduct.engine.api.select.SelectionsParser

class RequiredSelectionsAreAcyclicTest {
    @Test
    fun `valid -- no required selections`() {
        assertValid("type Subject { x: Int }")
    }

    @Test
    fun `valid -- dependency on sibling`() {
        assertValid(
            "type Subject { x: Int y: Int }",
            "Subject" to "x" to "y"
        )
    }

    @Test
    fun `valid -- dependency on nested sibling`() {
        assertValid(
            """
                type Obj { x: Int }
                type Subject { x: Int obj: Obj }
            """.trimIndent(),
            "Subject" to "x" to "obj { x }"
        )
    }

    @Test
    fun `valid -- interleaved sibling`() {
        assertValid(
            """
                type Obj { x: Int y: Int }
                type Subject { x: Int y: Int obj: Obj }
            """.trimIndent(),
            "Subject" to "x" to "obj { x }",
            "Subject" to "y" to "obj { y }",
        )
    }

    @Test
    fun `valid -- required selection sets on sub selections`() {
        assertValid(
            """
                type Obj { x: Int y: Int }
                type Subject { x: Int y: Int z: Int obj: Obj }
            """.trimIndent(),
            "Subject" to "x" to "y z",
            "Subject" to "y" to "obj { y }",
        )
    }

    @Test
    fun `valid -- required selection sets on sub selections with nested fields`() {
        assertValid(
            """
                type Obj { a: Int }
                type Subject { x: Int y: Obj z: Int h: Int }
            """.trimIndent(),
            "Subject" to "x" to "y { a } z",
            "Subject" to "y" to "h"
        )
    }

    @Test
    fun `valid -- same required selection sets for same coordinate`() {
        assertValid(
            "type Subject { x: Int y: Int }",
            "Subject" to "x" to "y",
            "Subject" to "x" to "y"
        )
    }

    @Test
    fun `valid -- same required selection sets for same coordinate 1`() {
        assertValid(
            "type Subject { x: Int y: Int z: Int }",
            "Subject" to "x" to "y",
            "Subject" to "x" to "y",
            "Subject" to "y" to "z",
        )
    }

    @Test
    fun `valid -- different required selection sets for same coordinate`() {
        assertValid(
            "type Subject { x: Int y: Int z: Int }",
            "Subject" to "x" to "y",
            "Subject" to "x" to "y z"
        )
    }

    @Test
    fun `valid -- different required selection sets for same coordinate 1`() {
        assertValid(
            "type Subject { x: Int y: Int z: Int }",
            "Subject" to "x" to "y",
            "Subject" to "x" to "z"
        )
    }

    @Test
    fun `valid -- same required selection sets for multiple same coordinate`() {
        assertValid(
            "type Subject { x: Int y: Int z: Int }",
            "Subject" to "x" to "y",
            "Subject" to "x" to "y",
            "Subject" to "y" to "z",
            "Subject" to "y" to "z",
        )
    }

    @Test
    fun `valid -- concrete system coordinate`() {
        assertValid(
            "type Subject { x: Int }",
            "Subject" to "x" to "__typename"
        )
    }

    @Test
    fun `valid -- selections on recursive object`() {
        assertValid(
            "type Subject { subj: Subject x: Int }",
            "Subject" to "x" to "subj { subj { subj { __typename } } }"
        )
    }

    @Test
    fun `valid -- required selections on co-recursive objects`() {
        assertValid(
            """
                type Other { subj: Subject }
                type Subject { other: Other, x: Int }
            """.trimIndent(),
            "Subject" to "x" to "other { subj { other { subj { __typename } } } }"
        )
    }

    @Test
    fun `valid -- required selections on co-recursive objects 1`() {
        assertValid(
            """
                type Obj { subj: Subject }
                type Subject { obj: Obj, x: Int }
            """.trimIndent(),
            "Subject" to "x" to "obj { subj { obj { subj { __typename } } } }"
        )
    }

    @Test
    fun `valid -- required selections on co-recursive objects 2`() {
        assertValid(
            """
                type Obj { subj:Subject }
                type Subject { obj:Obj, x:Int, y:Int }
            """.trimIndent(),
            "Subject" to "x" to "obj { subj { y } }",
            "Subject" to "y" to "obj { subj { __typename } }",
        )
    }

    @Test
    fun `valid -- abstract system coordinate`() {
        assertValid(
            """
                type Subject { x: Int u: Union }
                union Union = Subject
            """.trimIndent(),
            "Subject" to "x" to "u { __typename }"
        )
    }

    @Test
    fun `invalid -- recursion via self`() {
        assertInvalid(
            "type Subject { x: Int }",
            "Subject" to "x" to "x"
        )
    }

    @Test
    fun `invalid -- recursion via sibling fields`() {
        assertInvalid(
            "type Subject { x: Int, y: Int }",
            "Subject" to "x" to "y",
            "Subject" to "y" to "x",
        )
    }

    @Test
    fun `invalid -- recursion via nested object`() {
        assertInvalid(
            "type Subject { x: Int, y: Int, subject: Subject }",
            "Subject" to "x" to "subject { y }",
            "Subject" to "y" to "subject { x }",
        )
    }

    @Test
    fun `invalid -- recursion via nested object 1`() {
        assertInvalid(
            "type Subject { x: Int, y: Int, subject: Subject }",
            "Subject" to "x" to "y",
            "Subject" to "y" to "subject { x }",
        )
    }

    @Test
    fun `invalid -- recursion via nested object 2`() {
        assertInvalid(
            "type Subject { x: Subject }",
            "Subject" to "x" to "x { __typename }"
        )
    }

    @Test
    fun `invalid -- recursion via nested object for each coordinate`() {
        assertInvalid(
            "type Subject { x: Int, y: Int, subject: Subject }",
            "Subject" to "x" to "subject { x }",
            "Subject" to "y" to "subject { y }",
        )
    }

    @Test
    fun `invalid -- same coordinate with cycle itself`() {
        assertInvalid(
            "type Subject { x: Int }",
            "Subject" to "x" to "x",
            "Subject" to "x" to "x",
        )
    }

    @Test
    fun `invalid -- same coordinate recursion with cycle from nested object`() {
        assertInvalid(
            "type Subject { x: Int, y: Int, subject: Subject }",
            "Subject" to "x" to "y",
            "Subject" to "x" to "subject { y }",
            "Subject" to "y" to "subject { x }",
        )
    }

    @Test
    fun `invalid -- recursion via interface`() {
        assertInvalid(
            """
                type Subject implements Interface { x: Int, iface: Interface }
                interface Interface { x: Int }
            """.trimIndent(),
            "Subject" to "x" to "iface { x }",
        )
    }

    @Test
    fun `invalid -- recursion via interface 2`() {
        assertInvalid(
            """
                type Subject implements Interface { x: Int, iface: Interface }
                interface Interface { x: Int }
            """.trimIndent(),
            "Subject" to "x" to "iface { ... on Subject { x } }",
        )
    }

    @Test
    fun `invalid -- recursion via union`() {
        assertInvalid(
            """
                type Subject { x: Int, union: Union }
                union Union = Subject
            """.trimIndent(),
            "Subject" to "x" to "union { ... on Subject { x } }",
        )
    }

    @Test
    fun `invalid -- cycle through query selections`() {
        assertInvalid(
            sdl = "extend type Query { i:Int, f:Foo } type Foo { c:Int }",
            registry = MockRequiredSelectionSetRegistry.mk("Query" to "i" to "f { c }") +
                MockRequiredSelectionSetRegistry.mkForSelectedType("Query", "Foo" to "c" to "i")
        )
    }

    @Test
    fun `valid -- FromQueryFieldVariable without cycle`() {
        // The resolver for Subject.field depends on Query.data via FromQueryFieldVariable
        // Query.data has no dependencies, so no cycle exists
        assertDoesNotThrow {
            validateAll(
                "extend type Query { data:Int } type Subject { field(x:Int):Int, otherField(x:Int):Int }",
                MockRequiredSelectionSetRegistry.mk(
                    "Subject" to "field" to "otherField(x:\$varx)" to VariablesResolver.fromSelectionSetVariables(
                        ParsedSelections.empty("Subject"),
                        SelectionsParser.parse("Query", "data"),
                        listOf(
                            FromQueryFieldVariable("varx", "data")
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `valid -- FromObjectFieldVariable without cycle`() {
        // The resolver for Subject.field depends on Subject.data via FromObjectFieldVariable
        // Subject.data has no dependencies, so no cycle exists
        assertDoesNotThrow {
            validateAll(
                "type Subject { field(x:Int):Int, otherField(x:Int):Int, data:Int }",
                MockRequiredSelectionSetRegistry.mk(
                    "Subject" to "field" to "otherField(x:\$varx)" to VariablesResolver.fromSelectionSetVariables(
                        SelectionsParser.parse("Subject", "data"),
                        ParsedSelections.empty("Query"),
                        listOf(
                            FromObjectFieldVariable("varx", "data")
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `invalid -- recursion via VariableResolver required selections`() {
        // The resolver for Subject.a depends on the value of Subject.c, which is fetched using a variable derived from Subject.b
        // The resolver for Subject.b depends on the value of Subject.c, which is fetched using a variable derived from Subject.a
        assertInvalid(
            "type Subject { a(x:Int):Int, b(x:Int):Int, c(x:Int):Int }",
            MockRequiredSelectionSetRegistry.mk(
                "Subject" to "a" to "c(x:\$varx)" to VariablesResolver.fromSelectionSetVariables(
                    SelectionsParser.parse("Subject", "b(x:1)"),
                    ParsedSelections.empty("Query"),
                    listOf(
                        FromObjectFieldVariable("varx", "b")
                    )
                ),
                "Subject" to "b" to "c(x:\$varx)" to VariablesResolver.fromSelectionSetVariables(
                    SelectionsParser.parse("Subject", "a(x:2)"),
                    ParsedSelections.empty("Query"),
                    listOf(
                        FromObjectFieldVariable("varx", "a")
                    )
                )
            )
        )
    }

    @Test
    fun `invalid -- recursion via FromQueryFieldVariable required selections`() {
        // The resolver for Subject.a depends on a variable derived from Query.b
        // The resolver for Query.b depends on a variable derived from Query.a
        // This creates a cycle in the query field dependencies
        assertInvalid(
            "extend type Query { a(x:Int):Int, b(x:Int):Int } type Subject { field(x:Int):Int }",
            MockRequiredSelectionSetRegistry.mk(
                "Subject" to "field" to "field(x:\$varx)" to VariablesResolver.fromSelectionSetVariables(
                    ParsedSelections.empty("Subject"),
                    SelectionsParser.parse("Query", "b(x:1)"),
                    listOf(
                        FromQueryFieldVariable("varx", "b")
                    )
                ),
                "Query" to "b" to "a(x:\$vary)" to VariablesResolver.fromSelectionSetVariables(
                    ParsedSelections.empty("Query"),
                    SelectionsParser.parse("Query", "a(x:2)"),
                    listOf(
                        FromQueryFieldVariable("vary", "a")
                    )
                )
            )
        )
    }

    @Test
    fun `invalid -- mixed object and query field variable cycle`() {
        // Subject.a depends on Query.queryField via FromQueryFieldVariable
        // Query.queryField depends on Subject.b via object selection
        // Subject.b depends on Subject.a via object selection
        // This creates a cycle: Subject.a -> Query.queryField -> Subject.b -> Subject.a
        assertInvalid(
            "extend type Query { queryField(x:Int):Int } type Subject { a(x:Int):Int, b(x:Int):Int }",
            MockRequiredSelectionSetRegistry.mk(
                "Subject" to "a" to "a(x:\$varx)" to VariablesResolver.fromSelectionSetVariables(
                    ParsedSelections.empty("Subject"),
                    SelectionsParser.parse("Query", "queryField(x:1)"),
                    listOf(
                        FromQueryFieldVariable("varx", "queryField")
                    )
                ),
                "Query" to "queryField" to "queryField(x:\$vary)" to VariablesResolver.fromSelectionSetVariables(
                    SelectionsParser.parse("Subject", "b(x:2)"),
                    ParsedSelections.empty("Query"),
                    listOf(
                        FromObjectFieldVariable("vary", "b")
                    )
                ),
                "Subject" to "b" to "a(x:\$varz)" to VariablesResolver.fromSelectionSetVariables(
                    SelectionsParser.parse("Subject", "a(x:3)"),
                    ParsedSelections.empty("Query"),
                    listOf(
                        FromObjectFieldVariable("varz", "a")
                    )
                )
            )
        )
    }

    @Test
    fun `error path -- required selection traversal`() {
        val err = assertThrows<RequiredSelectionsCycleException> {
            validateAll(
                "type Subject { x: Int, y: Int }",
                "Subject" to "x" to "y",
                "Subject" to "y" to "x",
            )
        }
        err.assertErrorPath(
            "Subject" to "x",
            "Subject" to "y",
            "Subject" to "x"
        )
    }

    @Test
    fun `error path -- selection set traversal`() {
        val err = assertThrows<RequiredSelectionsCycleException> {
            validateAll(
                """
                    type Foo { x: Int bar: Bar }
                    type Bar { x: Int foo: Foo }
                """.trimIndent(),
                "Foo" to "x" to "bar { x }",
                "Bar" to "x" to "foo { x }"
            )
        }
        err.assertErrorPath(
            "Foo" to "x",
            "Foo" to "bar",
            "Bar" to "x",
            "Bar" to "foo",
            "Foo" to "x"
        )
    }

    @Test
    fun `error message for malformed selection set includes text of that selection set`() {
        val badSelectionSet = "{ someUniqueText {} }"
        val err = assertThrows<IllegalArgumentException> {
            validateAll(
                "type Foo { x: Int }",
                ("Foo" to "x") to badSelectionSet
            )
        }
        if (!err.message!!.contains(badSelectionSet)) {
            fail<Unit>("Message \"${err.message}\" does not contain \"$badSelectionSet\"")
        }
    }

    /**
     * Given a schema, plus a collection of required selection-sets,
     * asserts that our cycle detector does _not_ detect a cycle.
     * See [validateAll] for description of RSS representation.
     */
    private fun assertValid(
        sdl: String,
        vararg requiredSelectionSetEntries: Pair<Coordinate, String>
    ) {
        val registry = MockRequiredSelectionSetRegistry.mk(*requiredSelectionSetEntries)
        assertDoesNotThrow {
            validateAll(sdl, registry)
        }
    }

    /**
     * Similar to [assertValid], except that it asserts that our cycle
     * detector _does_ detect an error.
     */
    private fun assertInvalid(
        sdl: String,
        vararg requiredSelectionSetEntries: Pair<Coordinate, String>
    ) {
        val registry = MockRequiredSelectionSetRegistry.mk(*requiredSelectionSetEntries)
        assertThrows<RequiredSelectionsCycleException> {
            validateAll(sdl, registry)
        }
    }

    /**
     * Similar to [assertValid], except that it asserts that our cycle
     * detector _does_ detect an error.
     */
    private fun assertInvalid(
        sdl: String,
        registry: MockRequiredSelectionSetRegistry
    ) {
        assertThrows<RequiredSelectionsCycleException> {
            validateAll(sdl, registry)
        }
    }

    private fun RequiredSelectionsCycleException.assertErrorPath(vararg expectedPath: Coordinate) {
        assertEquals(expectedPath.toList(), this.path.toList())
    }

    private fun validateAll(
        sdl: String,
        vararg requiredSelectionSetEntries: Pair<Coordinate, String>
    ) = validateAll(sdl, MockRequiredSelectionSetRegistry.mk(*requiredSelectionSetEntries))

    /**
     * Given a schema and a collection of required selection sets,
     * runs the cycle-detection validator against that collection.
     *
     * Throws the validator's exception for the first RSS in the list
     * that does not pass validation, returns normally if all pass.
     *
     * The RSS collection is represented as a list of <coord, rss> pairs
     * where "coord" is a field coordinate and "rss" is the text of a
     * selection set on the type-dimension of the field coordinate.
     */
    private fun validateAll(
        sdl: String,
        registry: MockRequiredSelectionSetRegistry,
    ) {
        val schema = MockSchema.mk(
            """
            type Query { empty: Int }
            $sdl
            """.trimIndent()
        )
        val validator = RequiredSelectionsAreAcyclic(ViaductSchema(schema))

        val coordToValidate = registry.entries.map { it.coord }.toSet()
        coordToValidate.map { coord ->
            val ctx = RequiredSelectionsValidationCtx(
                coord,
                registry
            )
            validator.validate(ctx)
        }
    }
}
