@file:Suppress("ForbiddenImport")

package viaduct.testapps.fixtures

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Provides
import graphql.ExecutionResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import viaduct.service.api.ExecutionInput
import viaduct.service.api.Viaduct
import viaduct.service.api.spi.Flags
import viaduct.service.api.spi.mocks.MockFlagManager
import viaduct.service.runtime.MTDViaduct
import viaduct.service.runtime.SchemaRegistryBuilder
import viaduct.service.runtime.StandardViaduct
import viaduct.tenant.runtime.bootstrap.TenantPackageFinder
import viaduct.tenant.runtime.bootstrap.ViaductTenantAPIBootstrapper

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
 * @param customSchemaRegistration The custom schema registration function to use for the test application.
 */
@ExperimentalCoroutinesApi
class ViaductModernTestApplication(
    scopedSchemaInfo: Set<ScopedSchemaInfo>,
    fullSchemaRegex: String? = null,
    private val tenantPackageFinder: TenantPackageFinder,
    customSchemaRegistration: ((builder: SchemaRegistryBuilder) -> Unit)? = null
) {
    private val flagManager = MockFlagManager(Flags.useModernExecutionStrategyFlags + Flags.EXECUTE_ACCESS_CHECKS_IN_MODERN_EXECUTION_STRATEGY)

    private fun commonTenantPrefix() = tenantPackageFinder.tenantPackages().reduce { p, tenant -> p.commonPrefixWith(tenant) }

    private val schemaRegistryBuilder = SchemaRegistryBuilder().apply {
        if (customSchemaRegistration != null) {
            customSchemaRegistration(this)
        } else {
            withFullSchemaFromResources(commonTenantPrefix(), fullSchemaRegex)
            scopedSchemaInfo.forEach { (schemaId, scopesIds) ->
                registerScopedSchema(schemaId, scopesIds)
            }
        }
    }

    @Suppress("DEPRECATION")
    private val standardViaduct = StandardViaduct.Builder()
        .withFlagManager(flagManager)
        .withTenantAPIBootstrapperBuilder(ViaductTenantAPIBootstrapper.Builder().tenantPackageFinder(tenantPackageFinder))
        .withCheckerExecutorFactoryCreator { schema -> TestAppCheckerExecutorFactoryImpl(schema) }
        .withSchemaRegistryBuilder(schemaRegistryBuilder)
        .build()

    private val injector = Guice.createInjector(object : AbstractModule() {
        @Provides
        fun providesViaduct(mtdViaduct: MTDViaduct): Viaduct = mtdViaduct.routeUseWithCaution()
    })

    private val mtdViaduct = injector.getInstance(MTDViaduct::class.java)

    init {
        mtdViaduct.init(standardViaduct)
    }

    /**
     * Executes a query against the test application.
     *
     * @param scopeId The scope ID to use for the query.
     * @param query The query to execute.
     * @param variables The variables to use for the query.
     *
     * @return The result of the query execution.
     */
    fun execute(
        scopeId: String,
        query: String,
        variables: Map<String, Any?> = mapOf()
    ): ExecutionResult =
        runBlocking {
            val executionInput = ExecutionInput(
                query = query,
                variables = variables,
                requestContext = object {},
                schemaId = scopeId
            )
            mtdViaduct.executeAsync(executionInput).await()
        }

    fun beginHotSwap(
        scopedSchemaInfo: Set<ScopedSchemaInfo>,
        fullSchemaRegex: String,
    ) {
        val nextSchemaRegistryBuilder = SchemaRegistryBuilder().apply {
            withFullSchemaFromResources(commonTenantPrefix(), fullSchemaRegex)
            scopedSchemaInfo.forEach { (schemaId, scopesIds) ->
                registerScopedSchema(schemaId, scopesIds)
            }
        }
        val nextStandardViaduct = standardViaduct.newForSchema(nextSchemaRegistryBuilder)
        mtdViaduct.beginHotSwap(nextStandardViaduct)
    }

    fun endHotSwap() {
        mtdViaduct.endHotSwap()
    }
}

data class ScopedSchemaInfo(
    val schemaId: String,
    val scopeIds: Set<String>,
)
