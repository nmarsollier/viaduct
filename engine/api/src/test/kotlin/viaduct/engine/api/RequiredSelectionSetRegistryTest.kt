package viaduct.engine.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RequiredSelectionSetRegistryTest {
    @Test
    fun `Empty`() {
        val reg = RequiredSelectionSetRegistry.Empty
        assertEquals(listOf<RequiredSelectionSet>(), reg.getRequiredSelectionSets("Query", "__typename", true))
        assertEquals(listOf<RequiredSelectionSet>(), reg.getRequiredSelectionSets("Foo", "foo", true))
        assertEquals(listOf<RequiredSelectionSet>(), reg.getRequiredSelectionSets("", "", true))
        assertEquals(listOf<RequiredSelectionSet>(), reg.getRequiredSelectionSetsForType("Query", true))

        assertEquals(listOf<RequiredSelectionSet>(), reg.getRequiredSelectionSets("Query", "__typename", false))
        assertEquals(listOf<RequiredSelectionSet>(), reg.getRequiredSelectionSets("Foo", "foo", false))
        assertEquals(listOf<RequiredSelectionSet>(), reg.getRequiredSelectionSets("", "", false))
        assertEquals(listOf<RequiredSelectionSet>(), reg.getRequiredSelectionSetsForType("Query", false))
    }
}
