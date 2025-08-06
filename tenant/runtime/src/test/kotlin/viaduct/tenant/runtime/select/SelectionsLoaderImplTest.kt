@file:OptIn(ExperimentalCoroutinesApi::class)

package viaduct.tenant.runtime.select

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import viaduct.api.mocks.MockInternalContext
import viaduct.api.mocks.executionContext
import viaduct.api.select.SelectionSet
import viaduct.api.types.Query as QueryType
import viaduct.engine.api.RawSelectionSet
import viaduct.engine.api.RawSelectionsLoader
import viaduct.engine.api.ViaductSchema
import viaduct.engine.runtime.ProxyEngineObjectData
import viaduct.engine.runtime.select.RawSelectionSetFactoryImpl

class SelectionsLoaderImplTest {
    private val ssFactory = SelectionSetFactoryImpl(RawSelectionSetFactoryImpl(ViaductSchema(SelectTestFeatureAppTest.schema)))
    private val context = MockInternalContext(SelectTestFeatureAppTest.schema).executionContext

    @Test
    fun `loads empty selections`() =
        runBlockingTest {
            val rawSelectionsLoader = mockk<RawSelectionsLoader> {
                coEvery { load(any()) } returns mockk<ProxyEngineObjectData>()
            }
            SelectionsLoaderImpl<QueryType>(rawSelectionsLoader)
                .load(context, SelectionSet.empty(Query.Reflection))
        }

    @Test
    fun `loads empty selections 2`() =
        runBlockingTest {
            val rawSelectionsLoader = mockk<RawSelectionsLoader> {
                coEvery { load(any()) } returns mockk<ProxyEngineObjectData>()
            }
            SelectionsLoaderImpl<Query>(rawSelectionsLoader)
                .load(context, SelectionSetImpl(Query.Reflection, RawSelectionSet.empty(Query.Reflection.name)))
        }

    @Test
    fun `loads simple selections`() =
        runBlockingTest {
            val mockPED = mockk<ProxyEngineObjectData> {
                every { graphQLObjectType } returns SelectTestFeatureAppTest.schema.getObjectType(Query.Reflection.name)
                coEvery { fetch(Query.Reflection.Fields.intField.name) } returns 42
            }
            val rawSelectionsLoader = mockk<RawSelectionsLoader> {
                coEvery { load(any()) } returns mockPED
            }
            val loaded = SelectionsLoaderImpl<Query>(rawSelectionsLoader)
                .load(context, ssFactory.selectionsOn(Query.Reflection, Query.Reflection.Fields.intField.name))
            assertEquals(42, loaded.getIntField())
        }

    private val rawSSLoaderFactory = mockk<RawSelectionsLoader.Factory> {
        every { forQuery(any()) } returns mockk<RawSelectionsLoader>()
        every { forMutation(any()) } returns mockk<RawSelectionsLoader>()
    }

    @Test
    fun `Factory -- forQuery`() {
        val factory = SelectionsLoaderImpl.Factory(rawSSLoaderFactory)
        assertDoesNotThrow {
            factory.forQuery("myResolverId")
        }
    }

    @Test
    fun `Factory -- forMutation`() {
        val factory = SelectionsLoaderImpl.Factory(rawSSLoaderFactory)
        assertDoesNotThrow {
            factory.forMutation("myResolverId")
        }
    }
}
