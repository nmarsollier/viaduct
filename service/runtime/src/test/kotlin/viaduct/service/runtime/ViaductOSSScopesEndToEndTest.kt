@file:Suppress("ForbiddenImport")

package viaduct.service.runtime

import graphql.ExecutionResult
import graphql.schema.idl.RuntimeWiring
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import viaduct.service.api.ExecutionInput
import viaduct.service.api.spi.Flag
import viaduct.service.api.spi.FlagManager

/**
 * End-to-end tests for the Viaduct OSS interface.
 *
 * As we expand the OSS interface to include more of the Viaduct Modern surface area, these test will expand to cover
 * the end-to-end constract of the Viaduct OSS framework.
 */
@ExperimentalCoroutinesApi
class ViaductOSSScopesEndToEndTest {
    private lateinit var subject: StandardViaduct
    private lateinit var viaductSchemaRegistryBuilder: ViaductSchemaRegistryBuilder

    val flagManager = object : FlagManager {
        override fun isEnabled(flag: Flag) = true
    }

    val wiring = RuntimeWiring.MOCKED_WIRING // TODO: Replace with injected OSS/modern-only wiring

    @Test
    fun `Verify Builder using withFullSchemaFromSdl and registerSchema`() =
        runBlocking {
            val sdl = """
                extend type Query @scope(to: ["viaduct-public"]) { field: Int }
            """

            viaductSchemaRegistryBuilder = ViaductSchemaRegistryBuilder().withFullSchemaFromSdl(sdl).registerScopedSchema("public", setOf("viaduct-public"))
            subject = StandardViaduct.Builder()
                .withFlagManager(flagManager)
                .withNoTenantAPIBootstrapper()
                .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
                .build()

            val query = """
                    query TestQuery {
                        field
                    }
            """.trimIndent()
            val executionInput = ExecutionInput(query, "public", object {})

            val actual = subject.execute(executionInput)
            val actualAsynced = subject.executeAsync(executionInput).await()
            // Having an intermittent bug in synchronicity.  This is a workaround to ensure the execution

            val expected = ExecutionResult.newExecutionResult()
                .data(mapOf("field" to null))
                .build()

            assertEquals(expected.toSpecification(), actual.toSpecification())
            assertEquals(expected.toSpecification(), actualAsynced.toSpecification())
            // By the transitive property the following should never fail, but we're including it here out of paranoia
            assertEquals(actual.toSpecification(), actualAsynced.toSpecification())
        }

    @Test
    fun `Verify Builder using withFullSchemaFromFiles and registerSchema`() =
        runBlocking {
            viaductSchemaRegistryBuilder = ViaductSchemaRegistryBuilder().withFullSchemaFromResources()
                .registerScopedSchema("public", setOf("viaduct-public"))
            subject = StandardViaduct.Builder()
                .withFlagManager(flagManager)
                .withNoTenantAPIBootstrapper()
                .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
                .build()

            val query = """
                    query TestQuery {
                        field
                    }
            """.trimIndent()
            val executionInput = ExecutionInput(query, "public", object {})

            val actual = subject.execute(executionInput)
            val actualAsynced = subject.executeAsync(executionInput).await()
            val expected = ExecutionResult.newExecutionResult()
                .data(mapOf("field" to null))
                .build()

            assertEquals(expected.toSpecification(), actual.toSpecification())
            assertEquals(expected.toSpecification(), actualAsynced.toSpecification())
            // By the transitive property the following should never fail, but we're including it here out of paranoia
            assertEquals(actual.toSpecification(), actualAsynced.toSpecification())
        }

    @Test
    fun `Verify Builder using registerSchemaFromSdl and registerScopedSchema`() =
        runBlocking {
            val sdl = """
            extend type Query @scope(to: ["viaduct-public"]) { field: Int }
        """

            viaductSchemaRegistryBuilder = ViaductSchemaRegistryBuilder()
                .registerSchemaFromSdl("schema_id", sdl).registerScopedSchema("public", setOf("viaduct-public"))
            subject = StandardViaduct.Builder()
                .withFlagManager(flagManager)
                .withNoTenantAPIBootstrapper()
                .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
                .build()

            val query = """
                query TestQuery {
                    field
                }
            """.trimIndent()
            val executionInput = ExecutionInput(query, "public", object {})

            val actual = subject.execute(executionInput)
            val actualAsynced = subject.executeAsync(executionInput).await()
            // Having an intermittent bug in synchronicity. This is a workaround to ensure the execution

            val expected = ExecutionResult.newExecutionResult()
                .data(mapOf("field" to null))
                .build()

            assertEquals(expected.toSpecification(), actual.toSpecification())
            assertEquals(expected.toSpecification(), actualAsynced.toSpecification())
            // By the transitive property the following should never fail, but we're including it here out of paranoia
            assertEquals(actual.toSpecification(), actualAsynced.toSpecification())
        }

    @Test
    fun `Verify Builder using withFullSchemaFromSdl and registerScopedSchema`() =
        runBlocking {
            val sdl = """
            type Foo @scope(to: ["SCOPE1"]) {
                field: Int
            }

            interface Bar @scope(to: ["SCOPE1"]) {
                interfaceField: String
            }

            union MultiType @scope(to: ["SCOPE1"]) = Foo

            enum Status @scope(to: ["SCOPE1"]) {
                ACTIVE
                INACTIVE
            }

            input InputData @scope(to: ["SCOPE1"]) {
                inputField: String
            }

            extend type Query @scope(to: ["SCOPE1"]) {
                 testQuery: Foo
                 interfaceTest: Bar
                 unionTest: MultiType
                 enumTest: Status
            }

            extend type Query @scope(to: ["SCOPE1"]) {
                 extensionTest: String
            }
            """
            viaductSchemaRegistryBuilder = ViaductSchemaRegistryBuilder().withFullSchemaFromSdl(sdl)
                .registerScopedSchema("SCHEMA_ID", setOf("SCOPE1"))
            subject = StandardViaduct.Builder()
                .withFlagManager(flagManager)
                .withNoTenantAPIBootstrapper()
                .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
                .build()

            val query = """
                query TestQuery {
                    testQuery {
                        field
                    }
                    interfaceTest {
                        interfaceField
                    }
                    unionTest {
                        ... on Foo {
                            field
                        }
                    }
                    enumTest
                    extensionTest
                }
            """.trimIndent()
            val executionInput = ExecutionInput(query, "SCHEMA_ID", object {})
            val actualAsynced = subject.executeAsync(executionInput).await()
            val actual = subject.execute(executionInput)
            val expected = ExecutionResult.newExecutionResult()
                .data(
                    mapOf(
                        "testQuery" to null,
                        "interfaceTest" to null,
                        "unionTest" to null,
                        "enumTest" to null,
                        "extensionTest" to null
                    )
                )
                .build()
            assertEquals(expected.toSpecification(), actual.toSpecification())
            assertEquals(expected.toSpecification(), actualAsynced.toSpecification())
            // By the transitive property the following should never fail, but we're including it here out of paranoia
            assertEquals(actual.toSpecification(), actualAsynced.toSpecification())
        }

    @Test
    fun `Verify Builder using registerFullSchema`() =
        runBlocking {
            viaductSchemaRegistryBuilder = ViaductSchemaRegistryBuilder().registerFullSchema("public")
            subject = StandardViaduct.Builder()
                .withFlagManager(flagManager)
                .withNoTenantAPIBootstrapper()
                .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
                .build()

            val query = """
            query TestQuery {
                field
            }
            """.trimIndent()
            val executionInput = ExecutionInput(query, "public", object {})

            val actual = subject.execute(executionInput)
            val actualAsynced = subject.executeAsync(executionInput).await()
            // Having an intermittent bug in synchronicity. This is a workaround to ensure the execution

            val expected = ExecutionResult.newExecutionResult()
                .data(mapOf("field" to null))
                .build()

            assertEquals(expected.toSpecification(), actual.toSpecification())
            assertEquals(expected.toSpecification(), actualAsynced.toSpecification())
            // By the transitive property the following should never fail, but we're including it here out of paranoia
            assertEquals(actual.toSpecification(), actualAsynced.toSpecification())
        }

    @Test
    fun `Verify Builder using withScopedFuture`() =
        runBlocking {
            val sdl = """
                extend type Query @scope(to: ["viaduct-public"]) { field: Int }
            """
            viaductSchemaRegistryBuilder = ViaductSchemaRegistryBuilder().withFullSchemaFromSdl(sdl)
                .registerScopedSchema("public", setOf("viaduct-public"))
            subject = StandardViaduct.Builder()
                .withFlagManager(flagManager)
                .withNoTenantAPIBootstrapper()
                .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
                .build()

            val query = """
                query TestQuery {
                    field
                }
            """.trimIndent()
            val executionInput = ExecutionInput(query, "public", object {})

            val actual = subject.execute(executionInput)
            val actualAsynced = subject.executeAsync(executionInput).await()
            // Having an intermittent bug in synchronicity. This is a workaround to ensure the execution

            val expected = ExecutionResult.newExecutionResult()
                .data(mapOf("field" to null))
                .build()

            assertEquals(expected.toSpecification(), actual.toSpecification())
            assertEquals(expected.toSpecification(), actualAsynced.toSpecification())
            // By the transitive property the following should never fail, but we're including it here out of paranoia
            assertEquals(actual.toSpecification(), actualAsynced.toSpecification())
        }
}
