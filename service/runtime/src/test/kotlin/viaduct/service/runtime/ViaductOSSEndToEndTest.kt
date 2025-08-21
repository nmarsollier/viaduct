@file:Suppress("ForbiddenImport")

package viaduct.service.runtime

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.name.Named
import graphql.ExecutionResult
import graphql.GraphQL
import graphql.InvalidSyntaxError
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.ExecutionStrategy
import graphql.language.SourceLocation
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import viaduct.engine.api.CheckerExecutorFactory
import viaduct.engine.api.ViaductSchema
import viaduct.engine.api.coroutines.CoroutineInterop
import viaduct.engine.api.instrumentation.ChainedInstrumentation
import viaduct.engine.api.instrumentation.ViaductInstrumentationAdapter
import viaduct.engine.runtime.execution.WrappedCoroutineExecutionStrategy
import viaduct.engine.runtime.instrumentation.ResolverInstrumentation
import viaduct.engine.runtime.instrumentation.ScopeInstrumentation
import viaduct.service.api.ExecutionInput
import viaduct.service.api.spi.Flag
import viaduct.service.api.spi.FlagManager
import viaduct.utils.invariants.ClassTypeInvariant
import viaduct.utils.invariants.FieldInvariant
import viaduct.utils.invariants.FieldTypeInvariant
import viaduct.utils.invariants.typeInfo

/**
 * End-to-end tests for the Viaduct OSS interface.
 *
 * As we expand the OSS interface to include more of the Viaduct Modern surface area, these test will expand to cover
 * the end-to-end constract of the Viaduct OSS framework.
 */
@ExperimentalCoroutinesApi
class ViaductOSSEndToEndTest {
    private lateinit var subject: StandardViaduct
    private lateinit var viaductSchemaRegistryBuilder: ViaductSchemaRegistryBuilder

    val flagManager = object : FlagManager {
        override fun isEnabled(flag: Flag) = true
    }

    val wiring = RuntimeWiring.MOCKED_WIRING // TODO: Replace with injected OSS/modern-only wiring

    val schema = mkSchema(
        """
            directive @scope(to: [String!]!) repeatable on OBJECT | INPUT_OBJECT | ENUM | INTERFACE | UNION

            schema { query: Foo }
            type Foo @scope(to: ["viaduct-public"]) { field: Int }
        """.trimIndent()
    )

    @BeforeEach
    fun setUp() {
        viaductSchemaRegistryBuilder = ViaductSchemaRegistryBuilder().withFullSchema(schema).registerScopedSchema("public", setOf("viaduct-public"))
        subject = StandardViaduct.Builder()
            .withFlagManager(flagManager)
            .withNoTenantAPIBootstrapper()
            .withDataFetcherExceptionHandler(mockk())
            .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
            .build()
    }

    @Test
    fun `getAppliedScopes on public returns viaduct-public`() {
        val scopes = subject.getAppliedScopes("public")
        assertEquals(setOf("viaduct-public"), scopes)
    }

    @Test
    fun `getAppliedScopes on invalid returns null`() {
        val scopes = subject.getAppliedScopes("invalid")
        assertNull(scopes)
    }

    @Test
    fun `Viaduct with no instrumentations or wirings successfully returns null for valid query`() =
        runBlocking {
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
    fun `Viaduct with no instrumentations or wirings returns failure for invalid query`() =
        runBlocking {
            val query = "query"
            val executionInput = ExecutionInput(query, "public", object {})

            val actual = subject.executeAsync(executionInput).await()
            val expected = ExecutionResult.newExecutionResult()
                .errors(
                    listOf(
                        InvalidSyntaxError(SourceLocation(1, 6), "Invalid syntax with offending token '<EOF>' at line 1 column 6")
                    )
                )
                .data(null)
                .build()

            assertEquals(expected.toSpecification(), actual.toSpecification())
        }

    @Test
    fun `test injector properties from Viaduct Builder`() {
        // Mock the static method Guice.createInjector
        mockkStatic(Guice::class)
        lateinit var injector: Injector

        // Spy on the actual injector returned by Guice.createInjector
        every { Guice.createInjector(any<Iterable<Module>>()) } answers {
            injector = callOriginal()
            injector
        }

        // Create the Viaduct instance using the builder
        subject = StandardViaduct.Builder()
            .withFlagManager(flagManager)
            .withNoTenantAPIBootstrapper()
            .withDataFetcherExceptionHandler(mockk())
            .withSchemaRegistryBuilder(viaductSchemaRegistryBuilder)
            .build()

        val bindingsMap = injector.allBindings.map { (key, value) ->
            val keyString = key.annotation?.let { annotation ->
                if (annotation is Named) annotation.value else key.typeLiteral.rawType.simpleName
            } ?: key.typeLiteral.rawType.simpleName
            keyString to value.provider.get()
        }.toMap()
        val viaduct = injector.getInstance(StandardViaduct::class.java)
        val chainedInstrumentationInvariant = FieldInvariant(
            ChainedInstrumentation::class,
            mapOf(
                "instrumentations" to FieldTypeInvariant(
                    List::class,
                    listOf(
                        FieldInvariant(
                            ViaductInstrumentationAdapter::class,
                            mapOf("viaductInstrumentation" to ClassTypeInvariant(ScopeInstrumentation::class))
                        ),
                        ClassTypeInvariant(ResolverInstrumentation::class)
                    )
                )
            )
        )

        val viaductInvariant = FieldInvariant(
            StandardViaduct::class,
            mapOf(
                "queryExecutionStrategy" to ClassTypeInvariant(WrappedCoroutineExecutionStrategy::class),
                "mutationExecutionStrategy" to ClassTypeInvariant(WrappedCoroutineExecutionStrategy::class),
                "subscriptionExecutionStrategy" to ClassTypeInvariant(WrappedCoroutineExecutionStrategy::class),
                // This is to ensure it's not null.
                "viaductSchemaRegistry" to ClassTypeInvariant(ViaductSchemaRegistry::class),
                "chainedInstrumentation" to chainedInstrumentationInvariant
            )
        )

        viaductInvariant.check(viaduct)

        val viaductSchemaRegistry = injector.getInstance(ViaductSchemaRegistry::class.java)

        val viaductSchemaRegistryInvariant = FieldInvariant(
            ViaductSchemaRegistry::class,
            mapOf(
                "enginesById" to FieldTypeInvariant(
                    Map::class,
                    listOf(
                        FieldInvariant(
                            typeInfo<Lazy<ViaductSchemaRegistry.GraphQLEngine>>(),
                            mapOf(
                                "graphQL" to FieldInvariant(
                                    typeInfo<GraphQL>(),
                                    mapOf(
                                        "queryStrategy" to ClassTypeInvariant(WrappedCoroutineExecutionStrategy::class),
                                        "mutationStrategy" to ClassTypeInvariant(WrappedCoroutineExecutionStrategy::class),
                                        "subscriptionStrategy" to ClassTypeInvariant(WrappedCoroutineExecutionStrategy::class),
                                        "instrumentation" to chainedInstrumentationInvariant,
                                        "preparsedDocumentProvider" to ClassTypeInvariant(IntrospectionRestrictingPreparsedDocumentProvider::class),
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        viaductSchemaRegistryInvariant.check(viaductSchemaRegistry)

        // Define the expected properties using FieldInvariant
        val bindingsInvariant = FieldInvariant(
            Map::class,
            mapOf(
                "ViaductSchemaRegistry" to ClassTypeInvariant(ViaductSchemaRegistry::class),
                "FlagManager" to ClassTypeInvariant(FlagManager::class),
                "DataFetcherExceptionHandler" to ClassTypeInvariant(DataFetcherExceptionHandler::class),
                "CoroutineInterop" to ClassTypeInvariant(CoroutineInterop::class),
                "CheckerExecutorFactory" to ClassTypeInvariant(CheckerExecutorFactory::class),
                "QueryExecutionStrategy" to ClassTypeInvariant(ExecutionStrategy::class),
                "MutationExecutionStrategy" to ClassTypeInvariant(ExecutionStrategy::class),
                "SubscriptionExecutionStrategy" to ClassTypeInvariant(ExecutionStrategy::class)
            )
        )

        bindingsInvariant.check(bindingsMap)
    }

    private fun mkSchema(sdl: String): ViaductSchema = ViaductSchema(SchemaGenerator().makeExecutableSchema(SchemaParser().parse(sdl), wiring))
}
