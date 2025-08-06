package viaduct.testapps.resolver

import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertJson
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

/**
 * Tests other exceptions on @Resolver fields.
 */
class SchemaDefinitionTests : TestBase(
    setOf(ScopedSchemaInfo(DEFAULT_SCHEMA_ID, setOf(DEFAULT_PUBLIC_SCOPE_ID))),
    tenantPackageFinder = TestTenantPackageFinder(Tenants),
) {
    @Test
    fun `The schema field is not annotated with Resolver`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { notAnnotatedResolver }"
        ).assertJson("""{"data":{"notAnnotatedResolver":null}}""")
    }
}
