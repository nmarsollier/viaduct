package viaduct.testapps.resolver

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertJson
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

/**
 * Tests @Resolver on scalar queries.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScalarResolverTests : TestBase(
    setOf(ScopedSchemaInfo(DEFAULT_SCHEMA_ID, setOf(DEFAULT_PUBLIC_SCOPE_ID))),
    tenantPackageFinder = TestTenantPackageFinder(Tenants)
) {
    @Test
    fun `Resolver returns string`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { scalarString }"
        ).assertJson("""{"data":{"scalarString":"tenant1 value"}}""")
    }

    @Test
    fun `Resolver returns int`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { scalarInt }"
        ).assertJson("""{"data":{"scalarInt":123}}""")
    }

    @Test
    fun `Resolver with arguments returns string`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { scalarStringWithArgs(input: \"inputValue\") }"
        ).assertJson("""{"data":{"scalarStringWithArgs":"inputValue"}}""")
    }

    @Test
    fun `Resolver with optional arguments returns string`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { scalarStringWithArgs }"
        ).assertJson("""{"data":{"scalarStringWithArgs":"default"}}""")
    }

    @Test
    fun `Resolver query with arguments returns argument`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = """
                    query TestQuery(${'$'}variable: String) {
                        scalarStringWithArgs(input: ${'$'}variable)
                    }
                """,
            variables = mapOf("variable" to "inputValue")
        ).assertJson("""{"data":{"scalarStringWithArgs":"inputValue"}}""")
    }

    @Test
    fun `Resolver returns enum type`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { scalarEnum }"
        ).assertJson("""{"data":{"scalarEnum":"VALUE1"}}""")
    }

    @Test
    fun `Resolver dfp request private field`() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { scalarString2 }"
        ).assertJson("""{"data":{"scalarString2":"resolved: tenant1 private value"}}""")
    }
}
