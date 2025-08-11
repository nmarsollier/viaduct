package viaduct.tenant.runtime.select

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import viaduct.engine.api.RawSelectionSet

@ExperimentalCoroutinesApi
class SelectionSetFactoryImplTest : Assertions() {
    @Test
    fun `selectionsOn -- simple`() {
        val emptyRawSelectionSet = RawSelectionSet.empty("id")
        val factory = SelectionSetFactoryImpl(
            mockk {
                every {
                    rawSelectionSet(any(), any(), any())
                } returns emptyRawSelectionSet
            }
        )

        val ss = factory.selectionsOn(Foo.Reflection, "id", emptyMap())
        assertTrue(ss.isEmpty())
    }
}
