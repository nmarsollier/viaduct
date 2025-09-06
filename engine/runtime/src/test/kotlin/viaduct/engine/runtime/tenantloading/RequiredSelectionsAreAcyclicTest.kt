package viaduct.engine.runtime.tenantloading

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import viaduct.engine.api.FromObjectFieldVariable
import viaduct.engine.api.FromQueryFieldVariable
import viaduct.engine.api.ParsedSelections
import viaduct.engine.api.VariablesResolver
import viaduct.engine.api.mocks.MockRequiredSelectionSetRegistry
import viaduct.engine.api.mocks.MockRequiredSelectionSetRegistry.RequiredSelectionSetEntry.FieldCheckerEntry as FieldCheckerEntry
import viaduct.engine.api.mocks.MockRequiredSelectionSetRegistry.RequiredSelectionSetEntry.FieldResolverEntry as FieldResolverEntry
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
            FieldResolverEntry("Subject" to "x", "y")
        )
    }

    @Test
    fun `valid -- dependency on nested sibling`() {
        assertValid(
            """
                type Obj { x: Int }
                type Subject { x: Int obj: Obj }
            """.trimIndent(),
            FieldResolverEntry("Subject" to "x", "obj { x }")
        )
    }

    @Test
    fun `valid -- interleaved sibling`() {
        assertValid(
            """
                type Obj { x: Int y: Int }
                type Subject { x: Int y: Int obj: Obj }
            """.trimIndent(),
            FieldResolverEntry("Subject" to "x", "obj { x }"),
            FieldResolverEntry("Subject" to "y", "obj { y }"),
        )
    }

    @Test
    fun `valid -- required selection sets on sub selections`() {
        assertValid(
            """
                type Obj { x: Int y: Int }
                type Subject { x: Int y: Int z: Int obj: Obj }
            """.trimIndent(),
            FieldResolverEntry("Subject" to "x", "y z"),
            FieldResolverEntry("Subject" to "y", "obj { y }"),
        )
    }

    @Test
    fun `valid -- required selection sets on sub selections with nested fields`() {
        assertValid(
            """
                type Obj { a: Int }
                type Subject { x: Int y: Obj z: Int h: Int }
            """.trimIndent(),
            FieldResolverEntry("Subject" to "x", "y { a } z"),
            FieldResolverEntry("Subject" to "y", "h")
        )
    }

    @Test
    fun `valid -- same required selection sets for same coordinate`() {
        assertValid(
            "type Subject { x: Int y: Int }",
            FieldResolverEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "x", "y")
        )
    }

    @Test
    fun `valid -- same required selection sets for same coordinate 1`() {
        assertValid(
            "type Subject { x: Int y: Int z: Int }",
            FieldResolverEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "y", "z"),
        )
    }

    @Test
    fun `valid -- different required selection sets for same coordinate`() {
        assertValid(
            "type Subject { x: Int y: Int z: Int }",
            FieldResolverEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "x", "y z")
        )
    }

    @Test
    fun `valid -- different required selection sets for same coordinate 1`() {
        assertValid(
            "type Subject { x: Int y: Int z: Int }",
            FieldResolverEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "x", "z")
        )
    }

    @Test
    fun `valid -- same required selection sets for multiple same coordinate`() {
        assertValid(
            "type Subject { x: Int y: Int z: Int }",
            FieldResolverEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "y", "z"),
            FieldResolverEntry("Subject" to "y", "z"),
        )
    }

    @Test
    fun `valid -- concrete system coordinate`() {
        assertValid(
            "type Subject { x: Int }",
            FieldResolverEntry("Subject" to "x", "__typename")
        )
    }

    @Test
    fun `valid -- selections on recursive object`() {
        assertValid(
            "type Subject { subj: Subject x: Int }",
            FieldResolverEntry("Subject" to "x", "subj { subj { subj { __typename } } }")
        )
    }

    @Test
    fun `valid -- required selections on co-recursive objects`() {
        assertValid(
            """
                type Other { subj: Subject }
                type Subject { other: Other, x: Int }
            """.trimIndent(),
            FieldResolverEntry("Subject" to "x", "other { subj { other { subj { __typename } } } }")
        )
    }

    @Test
    fun `valid -- required selections on co-recursive objects 1`() {
        assertValid(
            """
                type Obj { subj: Subject }
                type Subject { obj: Obj, x: Int }
            """.trimIndent(),
            FieldResolverEntry("Subject" to "x", "obj { subj { obj { subj { __typename } } } }")
        )
    }

    @Test
    fun `valid -- required selections on co-recursive objects 2`() {
        assertValid(
            """
                type Obj { subj:Subject }
                type Subject { obj:Obj, x:Int, y:Int }
            """.trimIndent(),
            FieldResolverEntry("Subject" to "x", "obj { subj { y } }"),
            FieldResolverEntry("Subject" to "y", "obj { subj { __typename } }"),
        )
    }

    @Test
    fun `valid -- abstract system coordinate`() {
        assertValid(
            """
                type Subject { x: Int u: Union }
                union Union = Subject
            """.trimIndent(),
            FieldResolverEntry("Subject" to "x", "u { __typename }")
        )
    }

    @Test
    fun `valid -- field checker rss references self`() {
        assertValid(
            "type Subject { x: Int }",
            FieldCheckerEntry("Subject" to "x", "x"),
        )
    }

    @Test
    fun `valid -- field checker references self with resolver`() {
        assertValid(
            "type Subject { x: Int y: Int }",
            FieldCheckerEntry("Subject" to "x", "x"),
            FieldResolverEntry("Subject" to "x", "y"),
        )
    }

    @Test
    fun `valid -- field checker with multiple required selections`() {
        assertValid(
            "type Subject { x: Int y: Int z: Int }",
            FieldCheckerEntry("Subject" to "x", "y z")
        )
    }

    @Test
    fun `valid -- multiple field checkers for same field`() {
        assertValid(
            "type Subject { x: Int y: Int z: Int }",
            FieldCheckerEntry("Subject" to "x", "y"),
            FieldCheckerEntry("Subject" to "x", "z")
        )
    }

    @Test
    fun `valid -- field checker with resolver on different fields`() {
        assertValid(
            "type Subject { x: Int y: Int z: Int }",
            FieldCheckerEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "z", "y")
        )
    }

    @Test
    fun `valid -- mixed field checker and resolver dependencies`() {
        assertValid(
            "type Subject { x: Int y: Int z: Int }",
            FieldCheckerEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "y", "z")
        )
    }

    @Test
    fun `invalid -- recursion via self`() {
        assertInvalid(
            "type Subject { x: Int }",
            FieldResolverEntry("Subject" to "x", "x")
        )
    }

    @Test
    fun `invalid -- recursion via sibling fields`() {
        assertInvalid(
            "type Subject { x: Int, y: Int }",
            FieldResolverEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "y", "x"),
        )
    }

    @Test
    fun `invalid -- recursion via nested object`() {
        assertInvalid(
            "type Subject { x: Int, y: Int, subject: Subject }",
            FieldResolverEntry("Subject" to "x", "subject { y }"),
            FieldResolverEntry("Subject" to "y", "subject { x }"),
        )
    }

    @Test
    fun `invalid -- recursion via nested object 1`() {
        assertInvalid(
            "type Subject { x: Int, y: Int, subject: Subject }",
            FieldResolverEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "y", "subject { x }"),
        )
    }

    @Test
    fun `invalid -- recursion via nested object 2`() {
        assertInvalid(
            "type Subject { x: Subject }",
            FieldResolverEntry("Subject" to "x", "x { __typename }")
        )
    }

    @Test
    fun `invalid -- recursion via nested object for each coordinate`() {
        assertInvalid(
            "type Subject { x: Int, y: Int, subject: Subject }",
            FieldResolverEntry("Subject" to "x", "subject { x }"),
            FieldResolverEntry("Subject" to "y", "subject { y }"),
        )
    }

    @Test
    fun `invalid -- same coordinate with cycle itself`() {
        assertInvalid(
            "type Subject { x: Int }",
            FieldResolverEntry("Subject" to "x", "x"),
            FieldResolverEntry("Subject" to "x", "x"),
        )
    }

    @Test
    fun `invalid -- same coordinate recursion with cycle from nested object`() {
        assertInvalid(
            "type Subject { x: Int, y: Int, subject: Subject }",
            FieldResolverEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "x", "subject { y }"),
            FieldResolverEntry("Subject" to "y", "subject { x }"),
        )
    }

    @Test
    fun `invalid -- recursion via interface`() {
        assertInvalid(
            """
                type Subject implements Interface { x: Int, iface: Interface }
                interface Interface { x: Int }
            """.trimIndent(),
            FieldResolverEntry("Subject" to "x", "iface { x }"),
        )
    }

    @Test
    fun `invalid -- recursion via interface 2`() {
        assertInvalid(
            """
                type Subject implements Interface { x: Int, iface: Interface }
                interface Interface { x: Int }
            """.trimIndent(),
            FieldResolverEntry("Subject" to "x", "iface { ... on Subject { x } }"),
        )
    }

    @Test
    fun `invalid -- recursion via union`() {
        assertInvalid(
            """
                type Subject { x: Int, union: Union }
                union Union = Subject
            """.trimIndent(),
            FieldResolverEntry("Subject" to "x", "union { ... on Subject { x } }"),
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
    fun `invalid -- cycle after first element does not cause infinite loop`() {
        assertInvalid(
            """
                type Subject { x: Int, y: Int }
            """.trimIndent(),
            FieldResolverEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "y", "y"),
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
    fun `invalid -- field checker and resolver create cycle`() {
        assertInvalid(
            "type Subject { x: Int y: Int }",
            FieldCheckerEntry("Subject" to "x", "y"),
            FieldResolverEntry("Subject" to "y", "x")
        )
    }

    @Test
    fun `invalid -- field checker mixed with resolver cycle through nested object`() {
        assertInvalid(
            "type Subject { x: Int y: Int subject: Subject }",
            FieldCheckerEntry("Subject" to "x", "subject { y }"),
            FieldResolverEntry("Subject" to "y", "subject { x }")
        )
    }

    @Test
    fun `invalid -- cycle with field checker, resolver, and variables`() {
        // Field checker on Subject.a requires Subject.b
        // Resolver for Subject.b requires Subject.c with variable from Subject.a
        // This creates a cycle: Subject.a -> Subject.b -> Subject.a (via variable)
        assertInvalid(
            "type Subject { a(x:Int):Int, b:Int, c(x:Int):Int }",
            MockRequiredSelectionSetRegistry.mk(
                FieldCheckerEntry("Subject" to "a", "b"),
                FieldResolverEntry(
                    coord = "Subject" to "b",
                    selectionsType = "Subject",
                    selectionsString = "c(x:\$varx)",
                    variableProviders = VariablesResolver.fromSelectionSetVariables(
                        SelectionsParser.parse("Subject", "a(x:1)"),
                        null,
                        listOf(
                            FromObjectFieldVariable("varx", "a")
                        )
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
                FieldResolverEntry("Subject" to "x", "y"),
                FieldResolverEntry("Subject" to "y", "x"),
            )
        }
        err.assertErrorPath(
            "Subject.x",
            "Subject.y",
            "Subject.x"
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
                FieldResolverEntry("Foo" to "x", "bar { x }"),
                FieldResolverEntry("Bar" to "x", "foo { x }")
            )
        }
        err.assertErrorPath(
            "Foo.x",
            "Bar.x",
            "Foo.x"
        )
    }

    @Test
    fun `error message for malformed selection set includes text of that selection set`() {
        val badSelectionSet = "{ someUniqueText {} }"
        val err = assertThrows<IllegalArgumentException> {
            validateAll(
                "type Foo { x: Int }",
                FieldResolverEntry("Foo" to "x", badSelectionSet)
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
        vararg requiredSelectionSetEntries: MockRequiredSelectionSetRegistry.RequiredSelectionSetEntry
    ) {
        val registry = MockRequiredSelectionSetRegistry(requiredSelectionSetEntries.toList())
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
        vararg requiredSelectionSetEntries: MockRequiredSelectionSetRegistry.RequiredSelectionSetEntry
    ) {
        val registry = MockRequiredSelectionSetRegistry(requiredSelectionSetEntries.toList())
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

    private fun RequiredSelectionsCycleException.assertErrorPath(vararg expectedPath: String) {
        assertEquals(expectedPath.toList(), this.path.toList())
    }

    private fun validateAll(
        sdl: String,
        vararg requiredSelectionSetEntries: MockRequiredSelectionSetRegistry.RequiredSelectionSetEntry
    ) = validateAll(sdl, MockRequiredSelectionSetRegistry(requiredSelectionSetEntries.toList()))

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
        val validator = RequiredSelectionsAreAcyclic(schema)

        val coordToValidate = registry.entries
            .filterIsInstance<MockRequiredSelectionSetRegistry.RequiredSelectionSetEntry.FieldResolverEntry>()
            .map { it.coord }.filterNotNull().toSet()
        coordToValidate.map { coord ->
            val ctx = RequiredSelectionsValidationCtx(
                coord,
                registry
            )
            validator.validate(ctx)
        }
    }
}
