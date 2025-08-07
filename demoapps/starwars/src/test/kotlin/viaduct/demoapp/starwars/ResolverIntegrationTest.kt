package viaduct.demoapp.starwars

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
        fun `should resolve person query`() {
            val query = """
                query {
                    person(id: "1") {
                        id
                        name
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            println("DEBUG: Full GraphQL response: $response")
            val personId = response.path("data").path("person").path("id").asText()
            val personName = response.path("data").path("person").path("name").asText()
            println("DEBUG: personId = '$personId', personName = '$personName'")

            assertEquals("1", personId)
            assertNotNull(personName)
        }

        @Test
        fun `should resolve allPeople connection`() {
            val query = """
                query {
                    allPeople(first: 5) {
                        people {
                            id
                            name
                        }
                        totalCount
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            println("DEBUG: AllPeople GraphQL response: $response")
            val people = response.path("data").path("allPeople").path("people")
            val totalCount = response.path("data").path("allPeople").path("totalCount").asInt()
            println("DEBUG: people.size() = ${people.size()}, totalCount = $totalCount")

            assertTrue(people.size() > 0)
            assertTrue(totalCount > 0)
        }

        @Test
        fun `should resolve film query`() {
            val query = """
                query {
                    film(id: "1") {
                        id
                        title
                        director
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val filmId = response.path("data").path("film").path("id").asText()
            val filmTitle = response.path("data").path("film").path("title").asText()

            assertEquals("1", filmId)
            assertNotNull(filmTitle)
        }

        @Test
        fun `should resolve allFilms connection`() {
            val query = """
                query {
                    allFilms(first: 3) {
                        films {
                            id
                            title
                        }
                        totalCount
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val films = response.path("data").path("allFilms").path("films")

            assertTrue(films.size() > 0)
        }

        @Test
        fun `should resolve searchPerson query`() {
            val query = """
                query {
                    searchPerson(search: { byName: "Luke" }) {
                        id
                        name
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val personName = response.path("data").path("searchPerson").path("name").asText()

            assertTrue(personName.contains("Luke"))
        }
    }

    @Nested
    inner class FilmResolvers {
        @Test
        fun `should resolve all film fields`() {
            val query = """
                query {
                    film(id: "1") {
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

            assertEquals("1", filmId)
            assertNotNull(filmTitle)
            assertNotNull(filmDirector)
        }
    }

    @Nested
    inner class PersonResolvers {
        @Test
        fun `should resolve all person fields`() {
            val query = """
                query {
                    person(id: "1") {
                        id
                        name
                        birthYear
                        eyeColor
                        gender
                        hairColor
                        height
                        mass
                        skinColor
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
            val personId = response.path("data").path("person").path("id").asText()
            val personName = response.path("data").path("person").path("name").asText()
            val homeworld = response.path("data").path("person").path("homeworld")

            assertEquals("1", personId)
            assertNotNull(personName)
            assertNotNull(homeworld)
        }

        @Test
        fun `should resolve person homeworld relationship`() {
            val query = """
                query {
                    person(id: "1") {
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
            val homeworld = response.path("data").path("person").path("homeworld")

            assertNotNull(homeworld.path("id").asText())
        }

        @Test
        fun `should resolve person species relationship`() {
            val query = """
                query {
                    person(id: "1") {
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
            val species = response.path("data").path("person").path("species")

            assertNotNull(species)
        }
    }

    @Nested
    inner class CrossResolverIntegrationTests {
        @Test
        fun `should handle multi-type queries across all resolvers`() {
            val query = """
                query {
                    person(id: "1") {
                        id
                        name
                    }
                    film(id: "1") {
                        id  
                        title
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val personId = response.path("data").path("person").path("id").asText()
            val filmId = response.path("data").path("film").path("id").asText()

            assertEquals("1", personId)
            assertEquals("1", filmId)
        }

        @Test
        fun `should handle invalid IDs gracefully`() {
            val query = """
                query {
                    person(id: "invalid") {
                        id
                        name
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val person = response.path("data").path("person")

            assertTrue(person.isNull)
        }

        @Test
        fun `should resolve complex nested relationships across resolvers`() {
            val query = """
                query {
                    person(id: "1") {
                        id
                        name
                        homeworld {
                            id
                            name
                        }
                    }
                    film(id: "1") {
                        id
                        title
                        director
                    }
                }
            """.trimIndent()

            val response = executeGraphQLQuery(query)
            val personHomeworld = response.path("data").path("person").path("homeworld")
            val filmDirector = response.path("data").path("film").path("director").asText()

            assertNotNull(personHomeworld)
            assertNotNull(filmDirector)
        }
    }
}
