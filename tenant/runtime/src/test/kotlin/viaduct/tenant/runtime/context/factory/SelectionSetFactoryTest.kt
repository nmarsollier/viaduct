package viaduct.tenant.runtime.context.factory

import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldDefinition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import viaduct.api.select.SelectionSet
import viaduct.engine.api.Coordinate

@ExperimentalCoroutinesApi
class SelectionSetFactoryTest {
    private fun field(coord: Coordinate): GraphQLFieldDefinition =
        MockArgs.contextFactoryTestSchema.schema.getFieldDefinition(
            FieldCoordinates.coordinates(coord.first, coord.second)
        )

    private fun Factory<SelectionSetArgs, SelectionSet<*>>.barSelections(selections: String? = "x") =
        mk(MockArgs(typeName = "Bar", selectionString = selections).getSelectionSetArgs()).let {
            @Suppress("UNCHECKED_CAST")
            it as SelectionSet<Bar>
        }

    @Test
    fun noSelections() {
        assertEquals(
            SelectionSet.NoSelections,
            SelectionSetFactory.NoSelections.mk(MockArgs())
        )
    }

    @Test
    fun `forField -- not composite`() {
        assertEquals(
            SelectionSetFactory.NoSelections,
            SelectionSetFactory.forField(field("Bar" to "x"))
        )
    }

    @Test
    fun `forField -- composite`() {
        val selections = SelectionSetFactory.forField(field("Bar" to "bar")).barSelections()
        assertTrue(selections.contains(Bar.Reflection.Fields.x))
        assertFalse(selections.contains(Bar.Reflection.Fields.y))
    }

    @Test
    fun `forField -- list composite`() {
        val selections = SelectionSetFactory.forField(field("Bar" to "bars")).barSelections()
        assertTrue(selections.contains(Bar.Reflection.Fields.x))
        assertFalse(selections.contains(Bar.Reflection.Fields.y))
    }

    @Test
    fun forClass() {
        val selections = SelectionSetFactory.forClass(Bar::class).barSelections()
        assertTrue(selections.contains(Bar.Reflection.Fields.x))
        assertFalse(selections.contains(Bar.Reflection.Fields.y))
    }

    @Test
    fun forTypeName() {
        val selections = SelectionSetFactory.forTypeName("Bar").barSelections()
        assertTrue(selections.contains(Bar.Reflection.Fields.x))
        assertFalse(selections.contains(Bar.Reflection.Fields.y))
    }

    @Test
    fun forType() {
        val selections = SelectionSetFactory.forType(Bar.Reflection).barSelections()
        assertTrue(selections.contains(Bar.Reflection.Fields.x))
        assertFalse(selections.contains(Bar.Reflection.Fields.y))
    }

    @Test
    fun `forType -- no selections`() {
        assertEquals(
            SelectionSet.NoSelections,
            SelectionSetFactory.forType(Bar.Reflection).barSelections(null)
        )
    }
}
