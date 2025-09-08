package viaduct.demoapp.starwars

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import viaduct.api.grts.Character
import viaduct.api.grts.CharacterSearchInput
import viaduct.api.grts.Query_AllCharacters_Arguments
import viaduct.api.grts.Query_AllFilms_Arguments
import viaduct.api.grts.Query_AllPlanets_Arguments
import viaduct.api.grts.Query_AllSpecies_Arguments
import viaduct.api.grts.Query_AllVehicles_Arguments
import viaduct.api.grts.Query_SearchCharacter_Arguments
import viaduct.demoapp.starwars.data.StarWarsData
import viaduct.demoapp.starwars.resolvers.AllCharactersResolver
import viaduct.demoapp.starwars.resolvers.AllFilmsResolver
import viaduct.demoapp.starwars.resolvers.AllPlanetsResolver
import viaduct.demoapp.starwars.resolvers.AllSpeciesResolver
import viaduct.demoapp.starwars.resolvers.AllVehiclesResolver
import viaduct.demoapp.starwars.resolvers.SearchCharacterResolver
import viaduct.engine.api.ViaductSchema
import viaduct.engine.runtime.execution.DefaultCoroutineInterop
import viaduct.service.runtime.ViaductSchemaRegistryBuilder
import viaduct.tenant.testing.DefaultAbstractResolverTestBase

@OptIn(ExperimentalCoroutinesApi::class)
class QueryResolverUnitTests : DefaultAbstractResolverTestBase() {
    override fun getSchema(): ViaductSchema =
        ViaductSchemaRegistryBuilder()
            .withFullSchemaFromResources("viaduct.demoapp.starwars", ".*\\.graphqls")
            .build(DefaultCoroutineInterop)
            .getFullSchema()

    @Test
    fun `search character by name returns a matching character`() =
        runBlockingTest {
            val reference = StarWarsData.characters.first()
            val resolver = SearchCharacterResolver()

            val args = Query_SearchCharacter_Arguments.Builder(context)
                .search(
                    CharacterSearchInput.Builder(context)
                        .byName(reference.name.substring(0, 2))
                        .build()
                )
                .build()

            val result = runFieldResolver(
                resolver = resolver,
                arguments = args
            )

            assertNotNull(result)
            result!!
            assertEquals(reference.name, result.getName())
            assertEquals(reference.birthYear, result.getBirthYear())
        }

    @Test
    fun `search character by id returns exact character`() =
        runBlockingTest {
            val reference = StarWarsData.characters.first()
            val resolver = SearchCharacterResolver()

            val gid = context.globalIDFor(Character.Reflection, reference.id)

            val args = Query_SearchCharacter_Arguments.Builder(context)
                .search(
                    CharacterSearchInput.Builder(context)
                        .byId(gid)
                        .build()
                )
                .build()

            val result = runFieldResolver(
                resolver = resolver,
                arguments = args
            )

            assertNotNull(result)
            assertEquals(reference.name, result!!.getName())
        }

    @Test
    fun `allCharacters respects limit and maps fields`() =
        runBlockingTest {
            val limit = 3
            val resolver = AllCharactersResolver()

            val args = Query_AllCharacters_Arguments.Builder(context)
                .limit(limit)
                .build()

            val result = runFieldResolver(
                resolver = resolver,
                arguments = args
            )

            assertNotNull(result)
            assertEquals(limit, result!!.size)
            val ref = StarWarsData.characters.first()
            val first = result.first()!!
            assertEquals(ref.name, first.getName())
            assertEquals(ref.birthYear, first.getBirthYear())
        }

    @Test
    fun `allFilms respects limit and maps fields`() =
        runBlockingTest {
            val limit = 2
            val resolver = AllFilmsResolver()

            val args = Query_AllFilms_Arguments.Builder(context)
                .limit(limit)
                .build()

            val result = runFieldResolver(
                resolver = resolver,
                arguments = args
            )

            assertNotNull(result)
            assertEquals(limit, result!!.size)
            val ref = StarWarsData.films.first()
            val first = result.first()!!
            assertEquals(ref.title, first.getTitle())
            assertEquals(ref.episodeID, first.getEpisodeID())
        }

    @Test
    fun `allPlanets respects limit and maps fields`() =
        runBlockingTest {
            val limit = 4
            val resolver = AllPlanetsResolver()

            val args = Query_AllPlanets_Arguments.Builder(context)
                .limit(limit)
                .build()

            val result = runFieldResolver(
                resolver = resolver,
                arguments = args
            )

            assertNotNull(result)
            assertEquals(limit, result!!.size)
            val ref = StarWarsData.planets.first()
            val first = result.first()!!
            assertEquals(ref.name, first.getName())
        }

    @Test
    fun `allSpecies respects limit and maps fields`() =
        runBlockingTest {
            val limit = 1
            val resolver = AllSpeciesResolver()

            val args = Query_AllSpecies_Arguments.Builder(context)
                .limit(limit)
                .build()

            val result = runFieldResolver(
                resolver = resolver,
                arguments = args
            )

            assertNotNull(result)
            assertEquals(limit, result!!.size)
            val ref = StarWarsData.species.first()
            val first = result.first()!!
            assertEquals(ref.name, first.getName())
        }

    @Test
    fun `allVehicles respects limit and maps fields`() =
        runBlockingTest {
            val limit = 1
            val resolver = AllVehiclesResolver()

            val args = Query_AllVehicles_Arguments.Builder(context)
                .limit(limit)
                .build()

            val result = runFieldResolver(
                resolver = resolver,
                arguments = args
            )

            assertNotNull(result)
            assertEquals(limit, result!!.size)
            val ref = StarWarsData.vehicles.first()
            val first = result.first()!!
            assertEquals(ref.name, first.getName())
            assertEquals(ref.model, first.getModel())
        }
}
