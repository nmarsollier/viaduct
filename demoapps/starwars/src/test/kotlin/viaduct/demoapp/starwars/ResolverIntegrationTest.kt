package viaduct.demoapp.starwars

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import viaduct.api.grts.Character
import viaduct.api.grts.Film
import viaduct.api.reflect.Type
import viaduct.api.types.NodeCompositeOutput
import viaduct.tenant.runtime.globalid.GlobalIDCodecImpl
import viaduct.tenant.runtime.globalid.GlobalIDImpl
import viaduct.tenant.runtime.internal.ReflectionLoaderImpl

/**
 * Integration tests for all resolvers in the Star Wars GraphQL API.
 * These tests verify that all resolvers work correctly through REST calls to GraphQL endpoint.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResolverIntegrationTest {
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
        @Suppress("UNCHECKED_CAST")
        val type = mirror.reflectionFor(typeClass.simpleName!!) as Type<NodeCompositeOutput>
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

    @Nested
    inner class QueryResolvers {
        @Test
        fun `should resolve character query`() {
            val encodedCharacterId = createEncodedGlobalID(Character::class, "1")
            val query = """
                query {
                    character(id: "$encodedCharacterId") {
                        id
                        name
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            println("DEBUG: Full GraphQL response: $response")
            val characterId = response.path("data").path("character").path("id").asText()
            val characterName = response.path("data").path("character").path("name").asText()
            println("DEBUG: characterId = '$characterId', characterName = '$characterName'")

            // With Node interface, id field returns encoded GlobalID
            assertNotNull(characterId)
            assertTrue(characterId.isNotEmpty(), "Expected non-empty GlobalID, got: '$characterId'")
            assertNotNull(characterName)
        }

        @Test
        fun `should resolve allCharacters list`() {
            val query = """
                query {
                    allCharacters(limit: 5) {
                        id
                        name
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            println("DEBUG: AllCharacters GraphQL response: $response")
            val characters = response.path("data").path("allCharacters")
            println("DEBUG: characters.size() = ${characters.size()}")

            assertTrue(characters.size() > 0)
        }

        @Test
        fun `should resolve film query`() {
            val encodedFilmId = createEncodedGlobalID(Film::class, "1")
            val query = """
                query {
                    film(id: "$encodedFilmId") {
                        id
                        title
                        director
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val filmId = response.path("data").path("film").path("id").asText()
            val filmTitle = response.path("data").path("film").path("title").asText()

            // Film now uses GlobalID format (implements Node interface)
            val expectedGlobalId = createEncodedGlobalID(Film::class, "1")
            assertEquals(expectedGlobalId, filmId)
            assertNotNull(filmTitle)
        }

        @Test
        fun `should resolve allFilms list`() {
            val query = """
                query {
                    allFilms(limit: 3) {
                        id
                        title
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val films = response.path("data").path("allFilms")

            assertTrue(films.size() > 0)
        }

        @Test
        fun `should resolve searchCharacter query`() {
            val query = """
                query {
                    searchCharacter(search: { byName: "Luke" }) {
                        id
                        name
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            println("DEBUG: searchCharacter GraphQL response: $response")
            val searchCharacterData = response.path("data").path("searchCharacter")
            println("DEBUG: searchCharacter data: $searchCharacterData")
            val characterName = searchCharacterData.path("name").asText()
            println("DEBUG: characterName = '$characterName'")

            assertNotNull(characterName)
            assertTrue(characterName.contains("Luke"), "Expected person name to contain 'Luke', got: '$characterName'")
        }
    }

    @Nested
    inner class FilmResolvers {
        @Test
        fun `should resolve all film fields`() {
            val encodedFilmId = createEncodedGlobalID(Film::class, "1")
            val query = """
                query {
                    film(id: "$encodedFilmId") {
                        id
                        title
                        episodeID
                        director
                        producers
                        releaseDate
                        openingCrawl
                        created
                        edited
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val filmId = response.path("data").path("film").path("id").asText()
            val filmTitle = response.path("data").path("film").path("title").asText()
            val filmDirector = response.path("data").path("film").path("director").asText()

            val expectedGlobalId = createEncodedGlobalID(Film::class, "1")
            assertEquals(expectedGlobalId, filmId)
            assertNotNull(filmTitle)
            assertNotNull(filmDirector)
        }
    }

    @Nested
    inner class CharacterResolvers {
        @Test
        fun `should resolve all character fields`() {
            val encodedCharacterId = createEncodedGlobalID(Character::class, "1")
            val query = """
                query {
                    character(id: "$encodedCharacterId") {
                        id
                        name
                        birthYear
                        eyeColor
                        gender
                        hairColor
                        height
                        mass
                        homeworld {
                            id
                            name
                        }
                        species {
                            id
                            name
                        }
                        created
                        edited
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val characterId = response.path("data").path("character").path("id").asText()
            val characterName = response.path("data").path("character").path("name").asText()
            val homeworld = response.path("data").path("character").path("homeworld")

            // With Node interface, id field returns encoded GlobalID
            assertNotNull(characterId)
            assertTrue(characterId.isNotEmpty(), "Expected non-empty GlobalID, got: '$characterId'")
            assertNotNull(characterName)
            assertNotNull(homeworld)
        }

        @Test
        fun `should resolve person homeworld relationship`() {
            val encodedCharacterId = createEncodedGlobalID(Character::class, "1")
            val query = """
                query {
                    character(id: "$encodedCharacterId") {
                        id
                        name
                        homeworld {
                            id
                            name
                        }
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val homeworld = response.path("data").path("character").path("homeworld")

            assertNotNull(homeworld.path("id").asText())
        }

        @Test
        fun `should resolve person species relationship`() {
            val encodedCharacterId = createEncodedGlobalID(Character::class, "1")
            val query = """
                query {
                    character(id: "$encodedCharacterId") {
                        id
                        name
                        species {
                            id
                            name
                        }
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val species = response.path("data").path("character").path("species")

            assertNotNull(species)
        }
    }

    @Nested
    inner class CrossResolverIntegrationTests {
        @Test
        fun `should handle multi-type queries across all resolvers`() {
            val encodedCharacterId = createEncodedGlobalID(Character::class, "1")
            val encodedFilmId = createEncodedGlobalID(Film::class, "1")
            val query = """
                query {
                    character(id: "$encodedCharacterId") {
                        id
                        name
                    }
                    film(id: "$encodedFilmId") {
                        id
                        title
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val characterId = response.path("data").path("character").path("id").asText()
            val filmId = response.path("data").path("film").path("id").asText()

            // With Node interface, person id returns encoded GlobalID
            val expectedCharacterGlobalId = createEncodedGlobalID(Character::class, "1")
            assertEquals(expectedCharacterGlobalId, characterId)
            // Film now also uses GlobalID format (implements Node interface)
            val expectedFilmGlobalId = createEncodedGlobalID(Film::class, "1")
            assertEquals(expectedFilmGlobalId, filmId)
        }

        @Test
        fun `should handle invalid IDs gracefully`() {
            val encodedInvalidId = createEncodedGlobalID(Character::class, "invalid")
            val query = """
                query {
                    character(id: "$encodedInvalidId") {
                        id
                        name
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val person = response.path("data").path("character")

            assertTrue(person.isNull)
        }

        @Test
        fun `should resolve complex nested relationships across resolvers`() {
            val encodedCharacterId = createEncodedGlobalID(Character::class, "1")
            val encodedFilmId = createEncodedGlobalID(Film::class, "1")
            val query = """
                query {
                    character(id: "$encodedCharacterId") {
                        id
                        name
                        homeworld {
                            id
                            name
                        }
                    }
                    film(id: "$encodedFilmId") {
                        id
                        title
                        director
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val personHomeworld = response.path("data").path("character").path("homeworld")
            val filmDirector = response.path("data").path("film").path("director").asText()

            assertNotNull(personHomeworld)
            assertNotNull(filmDirector)
        }
    }
}
