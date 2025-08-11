package viaduct.engine.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.engine.api.mocks.MockEngineObjectData
import viaduct.engine.api.mocks.MockSchema

@OptIn(ExperimentalCoroutinesApi::class)
class EngineDataReaderTest {
    private val schema = MockSchema.mk(
        """
            type Query {
              int:Int
              str:String
              ints:[Int]
              query:Query
              queries:[Query]
            }
        """.trimIndent()
    )

    private fun mkData(map: Map<String, Any?>): EngineObjectData = MockEngineObjectData.wrap(schema.schema.queryType, map)

    @Test
    fun `read -- reading from empty path returns root object`() =
        runBlockingTest {
            mkData(emptyMap()).let { data ->
                assertSame(
                    data,
                    EngineDataReader(emptyList()).read(data)
                )
            }
        }

    @Test
    fun `path contains empty strings`() {
        assertThrows<IllegalArgumentException> {
            EngineDataReader(listOf(""))
        }
    }

    @Test
    fun `read -- read object field`() =
        runBlockingTest {
            assertEquals(
                1,
                EngineDataReader(listOf("int")).read(mkData(mapOf("int" to 1)))
            )
        }

    @Test
    fun `read -- read null field value`() =
        runBlockingTest {
            assertEquals(
                null,
                EngineDataReader(listOf("int")).read(mkData(mapOf("int" to null)))
            )
        }

    @Test
    fun `read -- read terminal list value`() =
        runBlockingTest {
            assertEquals(
                listOf(1, 2),
                EngineDataReader(listOf("ints"))
                    .read(mkData(mapOf("ints" to listOf(1, 2))))
            )
        }

    @Test
    fun `read -- traverse through null object`() =
        runBlockingTest {
            assertEquals(
                null,
                EngineDataReader(listOf("query", "int"))
                    .read(mkData(mapOf("query" to null)))
            )
        }

    @Test
    fun `read -- traverse through object`() =
        runBlockingTest {
            assertEquals(
                1,
                EngineDataReader(listOf("query", "int"))
                    .read(mkData(mapOf("query" to mapOf("int" to 1))))
            )
        }

    @Test
    fun `read -- traverse through non-object`() =
        runBlockingTest {
            assertThrows<IllegalStateException> {
                EngineDataReader(listOf("int", "x"))
                    .read(mkData(mapOf("int" to 1)))
            }
        }
}
