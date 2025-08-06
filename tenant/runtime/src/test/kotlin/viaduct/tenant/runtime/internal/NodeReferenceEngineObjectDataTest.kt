package viaduct.tenant.runtime.internal

import graphql.schema.GraphQLObjectType
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import viaduct.engine.api.NodeEngineObjectData
import viaduct.engine.api.UnsetSelectionException

@OptIn(ExperimentalCoroutinesApi::class)
class NodeReferenceEngineObjectDataTest {
    @Test
    fun testFetch() =
        runBlockingTest {
            val objectType = mockk<GraphQLObjectType>(relaxed = true)
            every { objectType.name } returns "test"
            val nodeEOD = mockk<NodeEngineObjectData>()
            every { nodeEOD.id } returns "1"
            every { nodeEOD.graphQLObjectType } returns objectType
            val eod = NodeReferenceEngineObjectData(nodeEOD)
            assertEquals("1", eod.fetch("id"))
            assertThrows(UnsetSelectionException::class.java) { runBlockingTest { eod.fetch("x") } }
        }
}
