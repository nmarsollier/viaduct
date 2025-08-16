@file:Suppress("ForbiddenImport")

package com.example.viadapp

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import viaduct.engine.runtime.execution.withThreadLocalCoroutineContext
import viaduct.service.api.ExecutionInput
import viaduct.service.runtime.StandardViaduct
import viaduct.service.runtime.ViaductSchemaRegistryBuilder
import viaduct.tenant.runtime.bootstrap.ViaductTenantAPIBootstrapper

const val SCHEMA_ID = "publicSchema"

fun main(argv: Array<String>) {
    val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
    rootLogger.level = Level.WARN

    // Create a Viaduct engine
    // Note to reviewers: this is the long-form of building an engine.  We plan on
    // having a shorter form with defaults.
    val viaduct = StandardViaduct.Builder()
        .withTenantAPIBootstrapperBuilder(
            ViaductTenantAPIBootstrapper.Builder()
                .tenantPackagePrefix("com.example.viadapp")
        )
        .withSchemaRegistryBuilder(
            ViaductSchemaRegistryBuilder()
                .withFullSchemaFromResources("", ".*graphqls")
                .registerFullSchema(SCHEMA_ID)
        ).build()

    // Create an execution input
    val executionInput = ExecutionInput(
        query = (
            argv.getOrNull(0)
                ?: """
                     query {
                         greeting
                     }
                """.trimIndent()
        ),
        variables = emptyMap(),
        requestContext = object {},
        schemaId = SCHEMA_ID
    )

    // Run the query
    val result = runBlocking {
        // Note to reviewers: in the future the next two scope functions
        // will go away
        coroutineScope {
            withThreadLocalCoroutineContext {
                viaduct.execute(executionInput)
            }
        }
    }

    // [toSpecification] converts to JSON as described in the GraphQL
    // specification.
    val mapper = ObjectMapper().writerWithDefaultPrettyPrinter()
    println(
        mapper.writeValueAsString(result.toSpecification())
    )
}
