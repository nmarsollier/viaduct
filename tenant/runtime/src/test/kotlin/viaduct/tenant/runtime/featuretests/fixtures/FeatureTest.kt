@file:Suppress("ForbiddenImport")

package viaduct.tenant.runtime.featuretests.fixtures

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Provides
import graphql.ExecutionResult
import graphql.GraphQLError
import io.mockk.mockk
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import viaduct.service.api.ExecutionInput
import viaduct.service.api.Viaduct
import viaduct.service.runtime.MTDViaduct
import viaduct.service.runtime.SchemaRegistryBuilder
import viaduct.service.runtime.StandardViaduct

/**
 * Test harness for the Viaduct Modern engine configured with in-memory resolvers.
 * Allows arbitrary schemas and resolvers to be tested using a Modern-only engine free from the legacy engine.
 * Registered resolvers, resolver bases, and GRTs do not have to have any particular package and can be declared inline,
 * e.g. see `ViaductModernEndToEndTests`
 *
 * Usage:
 * ```kotlin
 *    FeatureTestBuilder()
 *      .sdl(<schema>)
 *      // configure a resolver class for a schema field
 *      .resolver(Query::class, FooField::class, FooFieldResolver::class)
 *      // or configure a resolver function that uses GRTs
 *      .resolver("Query" to "bar") { Bar.newBuilder(it).value(2).build() }
 *      // or configure a simple resolver function that does not read or write GRTs
 *      .resolver("Query" to "baz") { mapOf("value" to 2) }
 *      .build()
 *      .assertJson("<expected-json-string>", "<query>")
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeatureTest(
    val standardViaduct: StandardViaduct,
) {
    private val injector = Guice.createInjector(object : AbstractModule() {
        @Provides
        fun providesViaduct(mtdViaduct: MTDViaduct): Viaduct = mtdViaduct.routeUseWithCaution()
    })

    private val mtdViaduct = injector.getInstance(MTDViaduct::class.java)

    init {
        mtdViaduct.init(standardViaduct)
    }

    private fun executeAsync(
        query: String,
        variables: Map<String, Any?> = mapOf()
    ): CompletableFuture<ExecutionResult> {
        val executionInput = ExecutionInput(
            query = query,
            variables = variables,
            requestContext = mockk(),
            schemaId = FeatureTestBuilder.SCHEMA_ID
        )
        return mtdViaduct.executeAsync(executionInput)
    }

    fun execute(
        query: String,
        variables: Map<String, Any?> = mapOf(),
    ): ExecutionResult {
        lateinit var result: ExecutionResult
        runBlocking {
            result = executeAsync(query, variables).await()
        }
        return result
    }

    fun assertJson(
        expectedJson: String,
        query: String,
        variables: Map<String, Any?> = mapOf(),
    ): Unit = execute(query, variables).assertJson(expectedJson)

    fun changeSchema(
        fullSchemaSdl: String,
        scopedSchemaSdl: String? = null
    ): FeatureTest {
        val nextSchemaRegistryBuilder = SchemaRegistryBuilder().withFullSchemaFromSdl(fullSchemaSdl)
            .apply {
                if (scopedSchemaSdl != null) {
                    registerSchemaFromSdl(FeatureTestBuilder.SCHEMA_ID, scopedSchemaSdl)
                } else {
                    registerFullSchema(FeatureTestBuilder.SCHEMA_ID)
                }
            }
        val nextViaduct = standardViaduct.newForSchema(nextSchemaRegistryBuilder)
        mtdViaduct.beginHotSwap(nextViaduct)
        // Calling endHotSwap or not doesn't make a difference at the moment, thus skip for now
        return this
    }
}

/**
 * Assert that this result serializes to same value as [expectedJson].
 *
 * @param expectedJson a JSON string. The string may use some short-hand conventions,
 *  including unquoted object keys, trailing commas, and comments
 */
fun ExecutionResult.assertJson(expectedJson: String) {
    val expected = try {
        mapper.readValue<Map<String, Any?>>(expectedJson)
    } catch (e: Exception) {
        throw IllegalArgumentException("Cannot parse expectedJson", e)
    }
    assertEquals(expected, toSpecification())
}

/**
 * Assert that data matches [expectedDataJson]. [checkErrors] can be used to perform
 * assertions on the errors list.
 */
fun ExecutionResult.assertData(
    expectedDataJson: String,
    checkErrors: (errors: List<GraphQLError>) -> Unit = {}
) {
    val expectedData = try {
        mapper.readValue<Map<String, Any?>>(expectedDataJson)
    } catch (e: Exception) {
        throw IllegalArgumentException("Cannot parse expectedDataJson", e)
    }
    assertEquals(expectedData, getData())
    checkErrors(errors)
}

// configure an ObjectMapper that allows parsing compact JSON
private val mapper: ObjectMapper = ObjectMapper()
    .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
    .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
    .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
