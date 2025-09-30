@file:Suppress("ForbiddenImport")

package viaduct.testapps.fixtures

import graphql.ExecutionResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import viaduct.api.bootstrap.ViaductTenantAPIBootstrapper
import viaduct.service.api.ExecutionInput
import viaduct.service.api.spi.Flags
import viaduct.service.api.spi.mocks.MockFlagManager
import viaduct.service.runtime.SchemaRegistryConfiguration
import viaduct.service.runtime.StandardViaduct
import viaduct.tenant.runtime.bootstrap.TenantPackageFinder

/**
 * Viaduct Modern test application.
 * Mimics the behavior of a Viaduct Modern-based server, used for testing.
 *
 * The default usage would be using the scopes and the fullSchemaRegex to configure the test application.
 *
 * For those tests related with schema loading, you can use the customSchemaRegistration to register a custom full schema and public schema.
 *
 * @param scopedSchemaInfo The set of info to used to register scoped schemas. Each info includes a schema ID and a set of scope IDs.
 * @param fullSchemaRegex The full schema regex to use for the test application.
 * @param customSchemaRegistryConfiguration The custom schema registry configuration to use for the test application.
 */
@ExperimentalCoroutinesApi
open class ViaductModernTestApplication(
    scopedSchemaInfo: Set<ScopedSchemaInfo>,
    fullSchemaRegex: String? = null,
    private val tenantPackageFinder: TenantPackageFinder,
    customSchemaRegistryConfiguration: SchemaRegistryConfiguration? = null
) {
    private val flagManager = MockFlagManager.mk(Flags.EXECUTE_ACCESS_CHECKS_IN_MODERN_EXECUTION_STRATEGY)

    protected fun commonTenantPrefix() = tenantPackageFinder.tenantPackages().reduce { p, tenant -> p.commonPrefixWith(tenant) }

    private val schemaRegistryConfiguration = customSchemaRegistryConfiguration
        ?: SchemaRegistryConfiguration.fromResources(
            commonTenantPrefix(),
            fullSchemaRegex,
            scopes = scopedSchemaInfo.map { (schemaId, scopesIds) ->
                SchemaRegistryConfiguration.ScopeConfig(schemaId, scopesIds)
            }.toSet()
        )

    @Suppress("DEPRECATION")
    protected val standardViaduct = StandardViaduct.Builder()
        .withFlagManager(flagManager)
        .withTenantAPIBootstrapperBuilder(ViaductTenantAPIBootstrapper.Builder().tenantPackageFinder(tenantPackageFinder))
        .withCheckerExecutorFactoryCreator { schema -> TestAppCheckerExecutorFactoryImpl(schema) }
        .withSchemaRegistryConfiguration(schemaRegistryConfiguration)
        .build()

    /**
     * Executes a query against the test application.
     *
     * @param scopeId The scope ID to use for the query.
     * @param query The query to execute.
     * @param variables The variables to use for the query.
     *
     * @return The result of the query execution.
     */
    open fun execute(
        scopeId: String,
        query: String,
        variables: Map<String, Any?> = mapOf()
    ): ExecutionResult =
        runBlocking {
            val executionInput = ExecutionInput.create(
                schemaId = scopeId,
                operationText = query,
                variables = variables,
            )
            standardViaduct.executeAsync(executionInput).await()
        }
}

data class ScopedSchemaInfo(
    val schemaId: String,
    val scopeIds: Set<String>,
)
