package viaduct.engine.api.mocks

import io.kotest.matchers.collections.shouldContainAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import viaduct.engine.api.FromObjectFieldVariable
import viaduct.engine.api.ParsedSelections
import viaduct.engine.api.RequiredSelectionSet
import viaduct.engine.api.VariablesResolver
import viaduct.engine.api.select.SelectionsParser

class MockRequiredSelectionSetRegistryTest {
    @Test
    fun `mk without variables`() {
        val reg = MockRequiredSelectionSetRegistry.mk(
            "Foo" to "f1" to "x",
            "Foo" to "f2" to "x y",
            "Bar" to "b1" to "x",
            "Bar" to null to "x",
        )

        assertEquals(listOf<RequiredSelectionSet>(), reg.getRequiredSelectionSetsForField("Foo", "other"))
        assertEquals(
            listOf(
                RequiredSelectionSet(
                    SelectionsParser.parse("Foo", "x"),
                    emptyList()
                )
            ),
            reg.getRequiredSelectionSetsForField("Foo", "f1")
        )

        assertEquals(
            listOf(
                RequiredSelectionSet(
                    SelectionsParser.parse("Foo", "x y"),
                    emptyList()
                )
            ),
            reg.getRequiredSelectionSetsForField("Foo", "f2")
        )
        assertEquals(
            listOf(
                RequiredSelectionSet(
                    SelectionsParser.parse("Bar", "x"),
                    emptyList()
                )
            ),
            reg.getRequiredSelectionSetsForField("Bar", "b1")
        )
        assertEquals(
            listOf(
                RequiredSelectionSet(
                    SelectionsParser.parse("Bar", "x"),
                    emptyList()
                )
            ),
            reg.getRequiredSelectionSetsForType("Bar")
        )
    }

    @Test
    fun `mk with variables`() {
        val reg = MockRequiredSelectionSetRegistry.mk(
            "Foo" to "f1" to "x" to emptyList(),
            "Foo" to "f2" to "x(arg:\$a), b" to (
                VariablesResolver.fromSelectionSetVariables(
                    SelectionsParser.parse("Foo", "b"),
                    ParsedSelections.empty("Query"),
                    listOf(FromObjectFieldVariable("a", "b"))
                )
            ),
            "Foo" to null to "x(arg:\$a), b" to (
                VariablesResolver.fromSelectionSetVariables(
                    SelectionsParser.parse("Foo", "b"),
                    ParsedSelections.empty("Query"),
                    listOf(FromObjectFieldVariable("a", "b"))
                )
            ),
        )

        assertEquals(listOf<RequiredSelectionSet>(), reg.getRequiredSelectionSetsForField("Foo", "other"))
        assertEquals(
            listOf(
                RequiredSelectionSet(
                    SelectionsParser.parse("Foo", "x"),
                    emptyList()
                )
            ),
            reg.getRequiredSelectionSetsForField("Foo", "f1")
        )

        val rssWithVariable = RequiredSelectionSet(
            SelectionsParser.parse("Foo", "x(arg:\$a), b"),
            VariablesResolver.fromSelectionSetVariables(
                SelectionsParser.parse("Foo", "b"),
                ParsedSelections.empty("Query"),
                listOf(
                    FromObjectFieldVariable("a", "b")
                )
            ),
        )
        assertEquals(
            listOf(
                rssWithVariable
            ),
            reg.getRequiredSelectionSetsForField("Foo", "f2")
        )
        assertEquals(
            listOf(
                rssWithVariable
            ),
            reg.getRequiredSelectionSetsForType("Foo")
        )
    }

    @Test
    fun `mkForSelectedType`() {
        val reg = MockRequiredSelectionSetRegistry.mkForSelectedType(
            "Query",
            "Foo" to "f1" to "x",
            "Foo" to null to "y"
        )
        val rsss = reg.getRequiredSelectionSetsForField("Foo", "f1")
        assertEquals(1, rsss.size)
        val rss = rsss.first()
        assertEquals("Query", rss.selections.typeName)

        val typeRsss = reg.getRequiredSelectionSetsForType("Foo")
        assertEquals(1, typeRsss.size)
        val typeRss = typeRsss.first()
        assertEquals("Query", typeRss.selections.typeName)
    }

    @Test
    fun `plus`() {
        val result = MockRequiredSelectionSetRegistry.mk(
            "Foo" to "a" to "x",
            "Foo" to "b" to "y",
            "Foo" to "b" to "y",
            "Foo" to null to "x"
        ) + MockRequiredSelectionSetRegistry.Companion.mk(
            "Foo" to "b" to "y2",
            "Foo" to "c" to "z",
            "Foo" to null to "z"
        )
        assertEquals(
            listOf(
                RequiredSelectionSet(
                    SelectionsParser.parse("Foo", "x"),
                    emptyList()
                )
            ),
            result.getRequiredSelectionSetsForField("Foo", "a")
        )
        result.getRequiredSelectionSetsForField("Foo", "b").shouldContainAll(
            listOf(
                RequiredSelectionSet(
                    SelectionsParser.parse("Foo", "y"),
                    emptyList()
                ),
                RequiredSelectionSet(
                    SelectionsParser.parse("Foo", "y"),
                    emptyList()
                ),
                RequiredSelectionSet(
                    SelectionsParser.parse("Foo", "y2"),
                    emptyList()
                )
            )
        )
        assertEquals(
            listOf(
                RequiredSelectionSet(
                    SelectionsParser.parse("Foo", "z"),
                    emptyList()
                )
            ),
            result.getRequiredSelectionSetsForField("Foo", "c")
        )
        result.getRequiredSelectionSetsForType("Foo").shouldContainAll(
            listOf(
                RequiredSelectionSet(
                    SelectionsParser.parse("Foo", "x"),
                    emptyList()
                ),
                RequiredSelectionSet(
                    SelectionsParser.parse("Foo", "z"),
                    emptyList()
                )
            )
        )
        assertEquals(
            listOf<RequiredSelectionSet>(),
            result.getRequiredSelectionSetsForField("Missing", "a")
        )
    }
}
