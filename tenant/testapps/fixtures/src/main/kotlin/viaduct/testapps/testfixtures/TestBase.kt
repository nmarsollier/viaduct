package viaduct.testapps.testfixtures

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.BeforeEach
import viaduct.service.runtime.SchemaRegistryConfiguration
import viaduct.tenant.runtime.bootstrap.TenantPackageFinder
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.ViaductModernTestApplication

/**
 * Base class for tests that use the Viaduct Modern test application.
 *
 * This class is responsible for setting up the test application and executing queries against it.
 *
 * The default usage would be using the scopes and the fullSchemaRegex to configure the test application.
 *
 * For those tests related with schema loading, you can use the customSchemaRegistration to register a custom full schema and public schema.
 *
 * @param scopedSchemaInfo The set of info to used to register scoped schemas. Each info includes a schema ID and a set of scope IDs.
 * @param customSchemaRegistryConfiguration The custom schema registry configuration to use for the test application.
 */
@ExperimentalCoroutinesApi
abstract class TestBase(
    private val scopedSchemaInfo: Set<ScopedSchemaInfo> = setOf(ScopedSchemaInfo("schemaId", setOf())),
    private val fullSchemaRegex: String? = null,
    private val tenantPackageFinder: TenantPackageFinder,
    private val customSchemaRegistryConfiguration: SchemaRegistryConfiguration? = null
) {
    protected lateinit var testApp: ViaductModernTestApplication

    @BeforeEach
    open fun beforeEach() {
        testApp = ViaductModernTestApplication(scopedSchemaInfo, fullSchemaRegex, tenantPackageFinder, customSchemaRegistryConfiguration)
    }

    /**
     * Executes a query against the test application.
     *
     * @param schemaId The scope ID to use for the query.
     * @param query The query to execute.
     * @param variables The variables to use for the query.
     *
     * @return The result of the query execution.
     */
    protected fun execute(
        schemaId: String,
        query: String,
        variables: Map<String, Any?> = mapOf()
    ) = testApp.execute(schemaId, query, variables)
}
