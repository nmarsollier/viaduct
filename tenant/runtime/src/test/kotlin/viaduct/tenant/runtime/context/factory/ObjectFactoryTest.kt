package viaduct.tenant.runtime.context.factory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import viaduct.api.types.Object

class ObjectFactoryTest {
    @Test
    fun `forClass -- missing ctor`() {
        assertThrows<IllegalArgumentException> {
            ObjectFactory.forClass(Object::class)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun forClass() =
        runBlockingTest {
            val obj = ObjectFactory.forClass(Bar::class)
                .mk(
                    MockArgs(
                        typeName = "Bar",
                        objectData = mapOf(
                            "x" to 42,
                            "y" to true,
                            "z" to "Z",
                            "bar" to mapOf("y" to true),
                            "bars" to listOf(mapOf("y" to true), mapOf("y" to false))
                        ),
                    ).getObjectArgs()
                )

            assertTrue(obj is Bar)
            obj as Bar
            assertEquals(42, obj.getX())
            assertEquals(true, obj.getY())
            assertEquals("Z", obj.getZ())
            obj.getBar()?.let {
                assertEquals(true, it.getY())
            } ?: fail("missing `bar` property")
            obj.getBars()?.let {
                assertEquals(2, it.size)
                assertEquals(true, it[0]!!.getY())
                assertEquals(false, it[1]!!.getY())
            } ?: fail("missing `bars` property")
        }
}
