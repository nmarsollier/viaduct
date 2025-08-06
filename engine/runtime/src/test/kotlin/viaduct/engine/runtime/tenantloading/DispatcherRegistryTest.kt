@file:OptIn(ExperimentalCoroutinesApi::class)

package viaduct.engine.runtime.tenantloading

import graphql.language.AstPrinter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import viaduct.engine.api.RequiredSelectionSet
import viaduct.engine.api.TenantAPIBootstrapper
import viaduct.engine.api.TenantModuleException
import viaduct.engine.api.ViaductSchema
import viaduct.engine.api.mocks.MockCheckerExecutor
import viaduct.engine.api.mocks.MockCheckerExecutorFactory
import viaduct.engine.api.mocks.MockTenantAPIBootstrapper
import viaduct.engine.api.mocks.MockTenantModuleBootstrapper
import viaduct.engine.api.mocks.Samples
import viaduct.engine.api.select.SelectionsParser
import viaduct.engine.runtime.DispatcherRegistry
import viaduct.engine.runtime.validation.Validator

class DispatcherRegistryTest {
    private lateinit var bootstrapper: TenantAPIBootstrapper
    private lateinit var checkerExecutorFactory: MockCheckerExecutorFactory

    @BeforeEach
    fun setUp() {
        bootstrapper = MockTenantAPIBootstrapper(listOf(Samples.mockTenantModule))

        checkerExecutorFactory = MockCheckerExecutorFactory(
            mapOf(
                Pair("TestType", "aField") to MockCheckerExecutor(
                    requiredSelectionSets = mapOf(
                        "checker_0" to RequiredSelectionSet(
                            SelectionsParser.parse("TestType", "dField"),
                            emptyList()
                        ),
                        "checker_1" to null
                    )
                ),
                Pair("TestType", "bIntField") to MockCheckerExecutor()
            ),
            mapOf(
                "TestNode" to MockCheckerExecutor()
            )
        )
    }

    private fun createDispatcherRegistry() = DispatcherRegistryFactory(bootstrapper, Validator.Unvalidated, checkerExecutorFactory).create(ViaductSchema(Samples.testSchema))

    @Test
    fun `test successful injection of DispatcherRegistry`() =
        runBlockingTest {
            val dispatcherRegistry = createDispatcherRegistry()
            // We have 5 resolvers: aField, bIntField, parameterizedField, cField, dField
            assertEquals(5, dispatcherRegistry.get().count())

            val objectType = Samples.testSchema.getObjectType("TestType")
            assertEquals("TestType", objectType.name)

            val resolverDispatcher = dispatcherRegistry.getFieldResolverDispatcher("TestType", "aField")
            assertNotNull(resolverDispatcher)
            val checkerExecutor = dispatcherRegistry.getCheckerExecutor("TestType", "aField")
            assertNotNull(checkerExecutor)

            val resolverDispatcherInt = dispatcherRegistry.getFieldResolverDispatcher("TestType", "bIntField")
            assertNotNull(resolverDispatcherInt)
            val checkerExecutorB = dispatcherRegistry.getCheckerExecutor("TestType", "bIntField")
            assertNotNull(checkerExecutorB)
        }

    @Test
    fun `test DispatcherRegistry getNodeCheckerExecutor`() {
        val dispatcherRegistry = createDispatcherRegistry()
        // absent
        assertEquals(null, dispatcherRegistry.getTypeCheckerExecutor("Other"))

        // present without a node resolver
        assertNull(dispatcherRegistry.getTypeCheckerExecutor("TestType"))

        // present with a node resolver
        assertNotNull(dispatcherRegistry.getTypeCheckerExecutor("TestNode"))
    }

    @Test
    fun `test DispatcherRegistry getNodeResolverDispatcher`() {
        val dispatcherRegistry = createDispatcherRegistry()
        // absent
        assertEquals(null, dispatcherRegistry.getNodeResolverDispatcher("Other"))

        // present without a node resolver
        assertEquals(
            null,
            dispatcherRegistry.getNodeResolverDispatcher("TestType")
        )

        // present with a node resolver
        val nodeResolver = dispatcherRegistry.getNodeResolverDispatcher("TestNode")
        assertTrue(nodeResolver != null)
    }

    @Test
    fun `test DispatcherRegistry getRequiredSelectionSet`() {
        val dispatcherRegistry = createDispatcherRegistry()
        // absent
        assertEquals(listOf<RequiredSelectionSet>(), dispatcherRegistry.getRequiredSelectionSets("Missing", "missing", true))
        assertEquals(listOf<RequiredSelectionSet>(), dispatcherRegistry.getRequiredSelectionSets("Missing", "missing", false))

        // present with required selections
        val required = dispatcherRegistry.getRequiredSelectionSets("TestType", "parameterizedField", true)
        assertTrue(required.isNotEmpty())
        assertEquals(1, required.size)
        assertTrue(
            AstPrinter
                .printAstCompact(required[0].selections.toDocument())
                .contains("fragment _ on TestType")
        )
        assertEquals(required, dispatcherRegistry.getRequiredSelectionSets("TestType", "parameterizedField", false))
    }

    @Test
    fun `test getRequiredSelectionSet combined with field checker Rss when executeAccessChecksInModstrat is true`() {
        val dispatcherRegistry = createDispatcherRegistry()
        val rss = dispatcherRegistry.getRequiredSelectionSets("TestType", "aField", true)
        assertTrue(rss.isNotEmpty())
        assertEquals(1, rss.size)
        assertEquals("TestType", rss[0].selections.typeName)
        assertTrue(
            AstPrinter
                .printAstCompact(rss[0].selections.toDocument())
                .contains("{dField}")
        )
    }

    @Test
    fun `test getRequiredSelectionSet combined with field checker Rss when executeAccessChecksInModstrat is off but checker is on modern field`() {
        val dispatcherRegistry = createDispatcherRegistry()
        val rss = dispatcherRegistry.getRequiredSelectionSets("TestType", "aField", false)
        assertTrue(rss.isNotEmpty())
        assertEquals(1, rss.size)
        assertEquals("TestType", rss[0].selections.typeName)
        assertTrue(
            AstPrinter
                .printAstCompact(rss[0].selections.toDocument())
                .contains("{dField}")
        )
    }

    internal class MockValidator : Validator<DispatcherRegistry> {
        var arg: DispatcherRegistry? = null

        override fun validate(t: DispatcherRegistry) {
            arg = t
        }
    }

    @Test
    fun `invokes validator`() {
        val bootstrapper = MockTenantAPIBootstrapper(emptyList())
        MockValidator().let { validator ->
            val wiring = DispatcherRegistryFactory(bootstrapper, validator, MockCheckerExecutorFactory()).create(ViaductSchema(Samples.testSchema))
            assertSame(wiring, validator.arg)
        }
    }

    @Test
    fun `bootstraps subset of bootstrappable tenants`() {
        // Create two modules - one empty and one with resolvers
        val emptyModule = MockTenantModuleBootstrapper(Samples.testSchema) { }
        val moduleWithResolvers = MockTenantModuleBootstrapper(Samples.testSchema) {
            field("TestType" to "aField") {
                resolver {
                    fn { _, _, _, _, _ -> "aField" }
                }
            }
            field("TestType" to "bIntField") {
                resolver {
                    fn { _, _, _, _, _ -> 42 }
                }
            }
            field("TestType" to "parameterizedField") {
                resolver {
                    fn { _, _, _, _, _ -> true }
                }
            }
            field("TestType" to "cField") {
                resolver {
                    fn { _, _, _, _, _ -> "cField" }
                }
            }
            field("TestType" to "dField") {
                resolver {
                    fn { _, _, _, _, _ -> "dField" }
                }
            }
        }

        val bootstrapper = MockTenantAPIBootstrapper(listOf(emptyModule, moduleWithResolvers))
        val wiring = MockValidator().let {
            DispatcherRegistryFactory(bootstrapper, it, MockCheckerExecutorFactory()).create(ViaductSchema(Samples.testSchema))
        }
        assertEquals(5, wiring.fieldResolverDispatchers.size)
    }

    @Test
    fun `test node batch resolver integration`() {
        val dispatcherRegistry = createDispatcherRegistry()

        // TestBatchNode should have a node resolver (wrapped BatchingNodeResolverDispatcherImpl)
        val batchNodeResolver = dispatcherRegistry.getNodeResolverDispatcher("TestBatchNode")
        assertNotNull(batchNodeResolver)
    }

    @Test
    @Suppress("DEPRECATION")
    @Disabled("Need to find a way to test this without creating engine->tenant dependencies.")
    fun `errors when schema mismatches tenants`() {
        val exception = assertThrows(TenantModuleException::class.java) {
            DispatcherRegistryFactory(
                MockTenantAPIBootstrapper(listOf(MockTenantModuleBootstrapper(Samples.testSchema))),
                Validator.Unvalidated,
                checkerExecutorFactory
            ).create(ViaductSchema(Samples.testSchema))
        }
        assertTrue(exception.message!!.startsWith("Refusing to create an empty executor registry for [viaduct.tenant.runtime.bootstrap.ViaductTenantModuleBootstrapper"))
    }
}
