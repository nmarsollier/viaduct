package viaduct.demoapp.starwars

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KClass
import org.junit.jupiter.api.Assertions.assertEquals
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
import viaduct.api.grts.Character
import viaduct.api.reflect.Type
import viaduct.api.types.CompositeOutput
import viaduct.tenant.runtime.globalid.GlobalIDCodecImpl
import viaduct.tenant.runtime.globalid.GlobalIDImpl
import viaduct.tenant.runtime.internal.ReflectionLoaderImpl

/**
 * Demonstration test for GlobalID functionality in the Star Wars GraphQL API.
 *
 * This test demonstrates:
 * 1. How Character objects implement the Node interface with encoded GlobalID
 * 2. How to query using string IDs and receive encoded GlobalIDs in response
 * 3. How GlobalID provides a unique, typed identifier for objects in the graph
 * 4. The correct pattern: query with string ID, get encoded GlobalID back
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GlobalIDDemoTest {
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

    private fun executeGraphQLQuery(query: String): JsonNode {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

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
    fun `should demonstrate GlobalID with Node interface`() {
        val encodedCharacterId = createEncodedGlobalID(Character::class, "1")
        val query = """
            query GetCharacterWithGlobalId {
              character(id: "$encodedCharacterId") {
                # With Node interface, id field returns encoded GlobalID
                id

                # Regular character fields
                name
                birthYear
                eyeColor
              }
            }
        """.trimIndent()

        val result = executeGraphQLQuery(query)

        // Verify no errors occurred
        assertEquals(
            true,
            result.get("errors")?.isNull ?: true,
            "GraphQL query should not return errors"
        )

        val character = result.get("data").get("character")
        assertNotNull(character, "Character should be found")

        // Verify basic character data
        assertEquals("Luke Skywalker", character.get("name").asText())
        assertEquals("19BBY", character.get("birthYear").asText())
        assertEquals("blue", character.get("eyeColor").asText())

        // Verify GlobalID (encoded format from Node interface)
        val characterId = character.get("id").asText()
        assertNotNull(characterId, "GlobalID should be present")
        assertTrue(characterId.isNotEmpty(), "GlobalID should not be empty")
        // GlobalID is encoded, so we just verify it's a valid non-empty string
    }

    @Test
    fun `should demonstrate GlobalID encoding with Node interface`() {
        // Query person using encoded GlobalID, get encoded GlobalID back
        val encodedCharacterId = createEncodedGlobalID(Character::class, "2")
        val query = """
            query {
              character(id: "$encodedCharacterId") {
                # Node interface id field returns encoded GlobalID
                id
                name
              }
            }
        """.trimIndent()

        val result = executeGraphQLQuery(query)

        // Verify no errors occurred
        assertEquals(
            true,
            result.get("errors")?.isNull ?: true,
            "GraphQL query should not return errors"
        )

        val character = result.get("data").get("character")
        assertNotNull(character, "Character should be found")

        // Verify person data
        assertEquals("Princess Leia", character.get("name").asText())

        // Verify encoded GlobalID
        val characterId = character.get("id").asText()
        assertNotNull(characterId, "GlobalID should be present")
        assertTrue(characterId.isNotEmpty(), "GlobalID should not be empty")

        // The GlobalID is encoded, so we verify it's a valid non-empty string
        // In the Node interface pattern, the id field contains the encoded GlobalID
    }

    @Test
    fun `should show GlobalID consistency across multiple characters`() {
        val query = """
            query GetMultipleCharactersWithGlobalIds {
              allCharacters(limit: 3) {
                # Node interface id field returns encoded GlobalID for each character
                id
                name
              }
            }
        """.trimIndent()

        val result = executeGraphQLQuery(query)

        // Verify no errors occurred
        assertEquals(
            true,
            result.get("errors")?.isNull ?: true,
            "GraphQL query should not return errors"
        )

        val characters = result.get("data").get("allCharacters")

        // Verify we got multiple characters
        assertEquals(3, characters.size(), "Should return 3 characters")

        // Verify each person has a valid encoded GlobalID
        for (personNode in characters) {
            val characterId = personNode.get("id").asText()
            val name = personNode.get("name").asText()

            // Verify encoded GlobalID is present and non-empty for each person
            assertNotNull(characterId, "GlobalID should be present for person: $name")
            assertTrue(characterId.isNotEmpty(), "GlobalID should not be empty for person: $name")

            // The GlobalID is encoded, so we just verify it's a valid non-empty string
            // Each person implementing Node interface gets their own encoded GlobalID
        }
    }
}
