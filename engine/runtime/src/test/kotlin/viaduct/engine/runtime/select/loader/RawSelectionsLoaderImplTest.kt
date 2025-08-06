@file:OptIn(ExperimentalCoroutinesApi::class)

package viaduct.engine.runtime.select.loader

import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import viaduct.engine.api.FragmentLoader
import viaduct.engine.api.RawSelectionSet
import viaduct.engine.api.ViaductSchema
import viaduct.engine.api.derived.DerivedFieldQueryMetadata
import viaduct.engine.api.fragment.Fragment
import viaduct.engine.api.fragment.FragmentFieldEngineResolutionResult
import viaduct.engine.runtime.MkMutationMetadata
import viaduct.engine.runtime.MkQueryMetadata
import viaduct.engine.runtime.RawSelectionsLoaderImpl
import viaduct.engine.runtime.select.RawSelectionSetFactoryImpl
import viaduct.engine.runtime.select.hash

class RawSelectionsLoaderImplTest {
    private val ssFactory = RawSelectionSetFactoryImpl(ViaductSchema(SelectTestSchemaFixture.schema))

    @Test
    fun `loads empty selections`() =
        runBlockingTest {
            RawSelectionsLoaderImpl(
                ViaductSchema(SelectTestSchemaFixture.schema),
                MockFragmentLoader.empty,
                MkQueryMetadata("myResolverId")
            ).load(RawSelectionSet.empty("Query"))
        }

    @Test
    fun `loads simple selections`() =
        runBlockingTest {
            val result = FragmentFieldEngineResolutionResult(mapOf("intField" to 42))
            val loader =
                RawSelectionsLoaderImpl(
                    ViaductSchema(SelectTestSchemaFixture.schema),
                    MockFragmentLoader(result),
                    MkQueryMetadata("myResolverId")
                )

            val selections = ssFactory.rawSelectionSet(
                typeName = "Query",
                "intField",
                mapOf()
            )
            val loaded = loader.load(selections)
            Assertions.assertEquals(42, loaded.fetch("intField"))
        }

    @Test
    fun `Factory -- forQuery`() {
        val factory = RawSelectionsLoaderImpl.Factory(MockFragmentLoader.empty, ViaductSchema(SelectTestSchemaFixture.schema))
        assertDoesNotThrow {
            factory.forQuery("myResolverId")
        }
    }

    @Test
    fun `MkQueryMetadata`() {
        val ss = RawSelectionSetFactoryImpl(ViaductSchema(SelectTestSchemaFixture.schema))
            .rawSelectionSet("Foo", "__typename", emptyMap())
        val metadata = MkQueryMetadata("myResolverId")(ss)

        Assertions.assertTrue(metadata.queryName.contains(ss.hash()))
        Assertions.assertTrue(metadata.onRootQuery)
        Assertions.assertFalse(metadata.onRootMutation)
        Assertions.assertEquals("myResolverId", metadata.classPath)
        Assertions.assertEquals("myResolverId", metadata.providerShortClasspath)
        Assertions.assertFalse(metadata.allowMutationOnQuery)
        Assertions.assertTrue(metadata.rootFieldName.isEmpty())
    }

    @Test
    fun `MkQueryMetadata -- empty resolverId`() {
        assertThrows<IllegalArgumentException> {
            MkQueryMetadata("")
        }
    }

    @Test
    fun `MkMutationMetadata`() {
        val ss = RawSelectionSetFactoryImpl(ViaductSchema(SelectTestSchemaFixture.schema))
            .rawSelectionSet("Foo", "__typename", emptyMap())
        val metadata = MkMutationMetadata("myResolverId")(ss)

        Assertions.assertTrue(metadata.queryName.contains(ss.hash()))
        Assertions.assertFalse(metadata.onRootQuery)
        Assertions.assertTrue(metadata.onRootMutation)
        Assertions.assertEquals("myResolverId", metadata.classPath)
        Assertions.assertEquals("myResolverId", metadata.providerShortClasspath)
        Assertions.assertFalse(metadata.allowMutationOnQuery)
        Assertions.assertTrue(metadata.rootFieldName.isEmpty())
    }

    @Test
    fun `MkMutationMetadata -- empty resolverId`() {
        assertThrows<IllegalArgumentException> {
            MkMutationMetadata("")
        }
    }
}

private class MockFragmentLoader(val result: FragmentFieldEngineResolutionResult) : FragmentLoader {
    override suspend fun loadFromEngine(
        fragment: Fragment,
        metadata: DerivedFieldQueryMetadata,
        source: Any?,
        dataFetchingEnvironment: DataFetchingEnvironment?
    ): FragmentFieldEngineResolutionResult = result

    companion object {
        val empty: FragmentLoader = MockFragmentLoader(FragmentFieldEngineResolutionResult.empty)
    }
}
