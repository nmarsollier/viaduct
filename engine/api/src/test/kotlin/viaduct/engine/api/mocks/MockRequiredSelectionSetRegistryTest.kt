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

        assertEquals(listOf<RequiredSelectionSet>(), reg.getRequiredSelectionSets("Foo", "other"))
        assertEquals(
            listOf(
                RequiredSelectionSet(
                    SelectionsParser.parse("Foo", "x"),
                    emptyList()
                )
            ),
            reg.getRequiredSelectionSets("Foo", "f1")
        )

        assertEquals(
            listOf(
                RequiredSelectionSet(
                    SelectionsParser.parse("Foo", "x y"),
                    emptyList()
                )
            ),
            reg.getRequiredSelectionSets("Foo", "f2")
        )
        assertEquals(
            listOf(
                RequiredSelectionSet(
                    SelectionsParser.parse("Bar", "x"),
                    emptyList()
                )
            ),
            reg.getRequiredSelectionSets("Bar", "b1")
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

        assertEquals(listOf<RequiredSelectionSet>(), reg.getRequiredSelectionSets("Foo", "other"))
        assertEquals(
            listOf(
                RequiredSelectionSet(
                    SelectionsParser.parse("Foo", "x"),
                    emptyList()
                )
            ),
            reg.getRequiredSelectionSets("Foo", "f1")
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
            reg.getRequiredSelectionSets("Foo", "f2")
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
        val rsss = reg.getRequiredSelectionSets("Foo", "f1")
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
            result.getRequiredSelectionSets("Foo", "a")
        )
        result.getRequiredSelectionSets("Foo", "b").shouldContainAll(
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
            result.getRequiredSelectionSets("Foo", "c")
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
            result.getRequiredSelectionSets("Missing", "a")
        )
    }
}
