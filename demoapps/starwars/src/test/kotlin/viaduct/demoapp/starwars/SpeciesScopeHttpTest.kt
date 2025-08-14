package viaduct.demoapp.starwars

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import viaduct.api.grts.Species
import viaduct.api.reflect.Type
import viaduct.api.types.CompositeOutput
import viaduct.tenant.runtime.globalid.GlobalIDCodecImpl
import viaduct.tenant.runtime.globalid.GlobalIDImpl
import viaduct.tenant.runtime.internal.ReflectionLoaderImpl

/**
 * HTTP-based test for Species scoping functionality
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpeciesScopeHttpTest {
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @LocalServerPort
    private var port: Int = 0

    private val objectMapper = ObjectMapper()

    // Setup GlobalID codec for creating expected GlobalID values in tests
    private val mirror = ReflectionLoaderImpl { name ->
        Class.forName("viaduct.api.grts.$name").kotlin
    }
    private val globalIDCodec = GlobalIDCodecImpl(mirror)

    private fun createEncodedGlobalID(
        typeClass: KClass<*>,
        internalId: String
    ): String {
        val type = mirror.reflectionFor(typeClass.simpleName!!) as Type<CompositeOutput>
        val globalId = GlobalIDImpl(type, internalId)
        return globalIDCodec.serialize(globalId)
    }

    private fun executeGraphQLQuery(
        query: String,
        scopes: Set<String>? = null
    ): JsonNode {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        // Add scope header if scopes are provided
        if (scopes != null) {
            headers.set("X-Viaduct-Scopes", scopes.joinToString(","))
        }

        val request = mapOf("query" to query)
        val entity = HttpEntity(request, headers)

        val response = restTemplate.postForEntity(
            "http://localhost:$port/graphql",
            entity,
            String::class.java
        )

        return objectMapper.readTree(response.body)
    }

    @Test
    fun `should resolve basic species fields without extras scope`() {
        val encodedSpeciesId = createEncodedGlobalID(Species::class, "1")
        val query = """
            query {
                species(id: "$encodedSpeciesId") {
                    id
                    name
                    classification
                    designation
                    averageHeight
                    averageLifespan
                    eyeColors
                    hairColors
                    language
                }
            }
        """.trimIndent()

        val response = executeGraphQLQuery(query)

        // Verify no errors occurred
        assertEquals(true, response.get("errors")?.isNull ?: true, "Query should execute without errors")

        val species = response.get("data").get("species")
        assertNotNull(species, "Species should be found")

        // Verify basic species data is available
        assertNotNull(species.get("name"), "Name should be available")
        assertEquals("Human", species.get("name").asText())
        assertNotNull(species.get("classification"), "Classification should be available")
        assertEquals("mammal", species.get("classification").asText())
    }

    @Test
    fun `should query extras fields when available`() {
        val encodedSpeciesId = createEncodedGlobalID(Species::class, "1")
        val query = """
            query {
                species(id: "$encodedSpeciesId") {
                    id
                    name
                    culturalNotes
                    rarityLevel
                    specialAbilities
                    technologicalLevel
                }
            }
        """.trimIndent()

        val response = executeGraphQLQuery(query, setOf("extras"))

        // This query should work since we're passing the extras scope
        assertFalse(response.has("errors") && !response.get("errors").isNull, "Query should not have errors")

        val species = response.get("data").get("species")
        assertNotNull(species, "Species should be found")
        assertNotNull(species.get("name"), "Name should be available")

        // Verify we get real backend data for extras fields
        assertEquals(
            "Diverse species with strong adaptability and technological advancement",
            species.get("culturalNotes")?.asText()
        )
        assertEquals("Common", species.get("rarityLevel")?.asText())
        assertTrue(species.get("specialAbilities")?.isArray == true)
    }

    @Test
    fun `should query extras fields even without extras scope`() {
        val encodedSpeciesId = createEncodedGlobalID(Species::class, "1")
        val query = """
            query {
                species(id: "$encodedSpeciesId") {
                    id
                    name
                    culturalNotes
                    rarityLevel
                    specialAbilities
                    technologicalLevel
                }
            }
        """.trimIndent()

        val response = executeGraphQLQuery(query) // No extras scope provided

        // With proper scoping, querying extras fields without scope should result in errors
        assertTrue(response.has("errors") && !response.get("errors").isNull, "Query should have errors when extras scope is not provided")

        // Verify that the error mentions the restricted fields or field access
        val errorMessage = response.get("errors").toString().lowercase()
        assertTrue(
            errorMessage.contains("culturalnotes") || errorMessage.contains("cultural") ||
                errorMessage.contains("rarity") || errorMessage.contains("special") ||
                errorMessage.contains("technological") || errorMessage.contains("scope") ||
                errorMessage.contains("field") || errorMessage.contains("access"),
            "Error message should indicate that extras fields are not accessible without proper scope: $errorMessage"
        )
    }
}
