package viaduct.tenant.runtime.select

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import viaduct.engine.api.ViaductSchema
import viaduct.engine.runtime.select.RawSelectionSetFactoryImpl

@ExperimentalCoroutinesApi
class SelectionSetFactoryImplTest : Assertions() {
    private val factory = SelectionSetFactoryImpl(
        RawSelectionSetFactoryImpl(ViaductSchema(SelectTestFeatureAppTest.schema))
    )

    @Test
    fun `selectionsOn -- simple`() {
        val ss = factory.selectionsOn(Foo.Reflection, "id", emptyMap())
        assertFalse(ss.isEmpty())
    }
}
