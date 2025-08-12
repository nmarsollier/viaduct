package viaduct.service.runtime

import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.engine.api.ViaductSchema
import viaduct.graphql.scopes.errors.SchemaScopeValidationError
import viaduct.service.api.spi.Flag
import viaduct.service.api.spi.FlagManager

/**
 * Test if missing scope in a query fails as expected.
 */
@ExperimentalCoroutinesApi
class ViaductOSSMissingScopeEndToEndTest {
    private lateinit var subject: StandardViaduct
    private lateinit var schemaRegistryBuilder: ViaductSchemaRegistryBuilder

    val flagManager = object : FlagManager {
        override fun isEnabled(flag: Flag) = true
    }

    val wiring = RuntimeWiring.MOCKED_WIRING // TODO: Replace with injected OSS/modern-only wiring

    val schema = mkSchema(
        """
            directive @scope(to: [String!]!) repeatable on OBJECT | INPUT_OBJECT | ENUM | INTERFACE | UNION
            directive @resolver on FIELD_DEFINITION | OBJECT

            type Query @scope(to: ["*"]) {
              _: String @deprecated
            }
            type Mutation @scope(to: ["*"]) {
              _: String @deprecated
            }
            type Subscription @scope(to: ["*"]) {
              _: String @deprecated
            }

            extend type Query @scope(to: ["publicScope"]) {
              helloWorld: String @resolver
            }

            extend type Query {
              scopeOmittedHelloWorld: String @resolver
            }
        """.trimIndent()
    )

    @BeforeEach
    fun setUp() {
        schemaRegistryBuilder = ViaductSchemaRegistryBuilder().withFullSchema(schema).registerScopedSchema("public", setOf("publicScope"))
    }

    @Test
    fun `Missing scope in query must fail`() {
        val exception = assertThrows<SchemaScopeValidationError> {
            subject = StandardViaduct.Builder()
                .withFlagManager(flagManager)
                .withNoTenantAPIBootstrapper()
                .withDataFetcherExceptionHandler(mockk())
                .withSchemaRegistryBuilder(schemaRegistryBuilder)
                .build()
        }
        assertEquals(
            "No scope directives found from node: 'ObjectTypeExtensionDefinition{name='Query', implements=[], directives=[], fieldDefinitions=[FieldDefinition{name='scopeOmittedHelloWorld'",
            exception.message?.substring(0, 175)
        )
    }

    private fun mkSchema(sdl: String): ViaductSchema = ViaductSchema(SchemaGenerator().makeExecutableSchema(SchemaParser().parse(sdl), wiring))
}
