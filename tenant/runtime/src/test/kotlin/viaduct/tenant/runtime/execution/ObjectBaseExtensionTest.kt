package viaduct.tenant.runtime.execution

import io.mockk.mockk
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import viaduct.api.internal.ObjectBase
import viaduct.engine.api.NodeEngineObjectData
import viaduct.tenant.runtime.internal.NodeReferenceEngineObjectData

class ObjectBaseExtensionTest {
    @Test
    fun `unwrap node reference returns node eod`() {
        val nodeEOD = mockk<NodeEngineObjectData>(relaxed = true)
        val eod = NodeReferenceEngineObjectData(nodeEOD)
        val grt = object : ObjectBase(mockk(), eod) {}
        assertEquals(nodeEOD, grt.unwrap())
    }

    @Test
    fun `unwrap grt returns node eod`() {
        val eod = mockk<NodeEngineObjectData>(relaxed = true)
        val grt = object : ObjectBase(mockk(), eod) {}
        assertEquals(eod, grt.unwrap())
    }
}
