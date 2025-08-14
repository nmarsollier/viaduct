package viaduct.demoapp.starwars

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KClass
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
 * Simple test to debug GlobalID implementation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SimpleGlobalIDTest {
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
    fun `should return encoded GlobalID for character query`() {
        val encodedCharacterId = createEncodedGlobalID(Character::class, "1")
        val query = """
            query {
              character(id: "$encodedCharacterId") {
                id
                name
              }
            }
        """.trimIndent()

        val result = executeGraphQLQuery(query)

        // Verify the response structure
        val data = result.get("data")
        org.junit.jupiter.api.Assertions.assertNotNull(data, "Data should not be null")

        val character = data.get("character")
        org.junit.jupiter.api.Assertions.assertNotNull(character, "Character should not be null")

        // Verify the GlobalID is encoded properly using GlobalIDImpl
        val globalId = character.get("id").asText()
        val expectedGlobalId = createEncodedGlobalID(Character::class, "1")
        org.junit.jupiter.api.Assertions.assertEquals(expectedGlobalId, globalId, "GlobalID should match expected encoded value")

        // Verify the name is correct
        val name = character.get("name").asText()
        org.junit.jupiter.api.Assertions.assertEquals("Luke Skywalker", name, "Name should match expected value")
    }
}
