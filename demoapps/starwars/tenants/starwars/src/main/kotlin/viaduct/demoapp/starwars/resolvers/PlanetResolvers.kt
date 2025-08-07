package viaduct.demoapp.starwars.resolvers

import viaduct.api.Resolver
import viaduct.demoapp.starwars.data.StarWarsData
import viaduct.demoapp.starwars.resolverbases.PlanetResolvers

/**
 * Resolvers for Planet type fields
 * Only id field needs a resolver since other fields are handled automatically
 */

@Resolver
class PlanetIdResolver : PlanetResolvers.Id() {
    override suspend fun resolve(ctx: Context): String {
        val planetId = ctx.objectValue.getId()
        return StarWarsData.planets.find { it.id == planetId }?.id ?: throw IllegalStateException("Planet not found")
    }
}

@Resolver
class PlanetRotationPeriodResolver {
    fun resolve(planet: StarWarsData.Planet): Int? {
        return planet.rotationPeriod
    }
}

@Resolver
class PlanetOrbitalPeriodResolver {
    fun resolve(planet: StarWarsData.Planet): Int? {
        return planet.orbitalPeriod
    }
}

@Resolver
class PlanetGravityResolver {
    fun resolve(planet: StarWarsData.Planet): Float? {
        return planet.gravity
    }
}

@Resolver
class PlanetPopulationResolver {
    fun resolve(planet: StarWarsData.Planet): Float? {
        return planet.population
    }
}

@Resolver
class PlanetClimatesResolver {
    fun resolve(planet: StarWarsData.Planet): List<String> {
        return planet.climates
    }
}

@Resolver
class PlanetTerrainsResolver {
    fun resolve(planet: StarWarsData.Planet): List<String> {
        return planet.terrains
    }
}

@Resolver
class PlanetSurfaceWaterResolver {
    fun resolve(planet: StarWarsData.Planet): Float? {
        return planet.surfaceWater
    }
}

@Resolver
class PlanetCreatedResolver {
    fun resolve(planet: StarWarsData.Planet): String {
        return planet.created.toString()
    }
}

@Resolver
class PlanetEditedResolver {
    fun resolve(planet: StarWarsData.Planet): String {
        return planet.edited.toString()
    }
}

// Connection resolvers
@Resolver
class PlanetResidentConnectionResolver {
    fun resolve(
        planet: StarWarsData.Planet,
        first: Int?,
        after: String?,
        last: Int?,
        before: String?
    ): PlanetResidentConnection {
        // Find people who have this planet as their homeworld
        val residents = StarWarsData.people.filter { it.homeworldId == planet.id }
        return PlanetResidentConnection(residents, first, after, last, before)
    }
}

@Resolver
class PlanetFilmConnectionResolver {
    fun resolve(
        planet: StarWarsData.Planet,
        first: Int?,
        after: String?,
        last: Int?,
        before: String?
    ): PlanetFilmConnection {
        // For simplicity, return all films for demonstration
        return PlanetFilmConnection(StarWarsData.films, first, after, last, before)
    }
}

// Connection classes
data class PlanetResidentConnection(
    private val residents: List<StarWarsData.Person>,
    private val first: Int?,
    private val after: String?,
    private val last: Int?,
    private val before: String?
) {
    fun getResidents(): List<StarWarsData.Person> = residents.take(first ?: 10)

    fun getTotalCount(): Int = residents.size

    fun hasNextPage(): Boolean = residents.size > (first ?: 10)

    fun hasPreviousPage(): Boolean = false
}

data class PlanetFilmConnection(
    private val films: List<StarWarsData.Film>,
    private val first: Int?,
    private val after: String?,
    private val last: Int?,
    private val before: String?
) {
    fun getFilms(): List<StarWarsData.Film> = films.take(first ?: 10)

    fun getTotalCount(): Int = films.size

    fun hasNextPage(): Boolean = films.size > (first ?: 10)

    fun hasPreviousPage(): Boolean = false
}
