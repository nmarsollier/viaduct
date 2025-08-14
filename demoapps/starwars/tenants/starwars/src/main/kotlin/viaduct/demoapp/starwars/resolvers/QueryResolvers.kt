package viaduct.demoapp.starwars.resolvers

import viaduct.api.Resolver
import viaduct.api.grts.Character
import viaduct.api.grts.Film
import viaduct.api.grts.Planet
import viaduct.api.grts.Species
import viaduct.api.grts.Starship
import viaduct.api.grts.Vehicle
import viaduct.demoapp.starwars.Constants.DEFAULT_PAGE_SIZE
import viaduct.demoapp.starwars.data.StarWarsData
import viaduct.demoapp.starwars.resolverbases.QueryResolvers

/**
 * Query resolvers for the Star Wars GraphQL API.
 *
 * The Query type demonstrates the @scope directive which restricts schema access
 * to specific tenants or contexts. All resolvers here are scoped to "starwars".
 */

/**
 * @resolver directive: Custom field resolution for Query.searchCharacter
 * @oneOf directive: The search input uses @oneOf directive, ensuring exactly one
 *                  search criteria is provided. This demonstrates input validation
 *                  where only one of byName, byId, or byBirthYear can be specified.
 */
@Resolver
class SearchCharacterResolver : QueryResolvers.SearchCharacter() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Character? {
        val search = ctx.arguments.search
        val byName = search.byName
        val byId = search.byId
        val byBirthYear = search.byBirthYear

        val character = when {
            byName != null -> StarWarsData.characters.find { it.name.contains(byName, ignoreCase = true) }
            byId != null -> StarWarsData.characters.find { it.id == byId.internalID }
            byBirthYear != null -> StarWarsData.characters.find { it.birthYear == byBirthYear }
            else -> null
        }

        return if (character != null) {
            Character.Builder(ctx)
                .id(ctx.globalIDFor(Character.Reflection, character.id))
                .name(character.name)
                .birthYear(character.birthYear)
                .eyeColor(character.eyeColor)
                .gender(character.gender)
                .hairColor(character.hairColor)
                .height(character.height)
                .mass(character.mass?.toDouble())
                .created(character.created.toString())
                .edited(character.edited.toString())
                .build()
        } else {
            null
        }
    }
}

/**
 * @resolver directive: Custom field resolution for Query.allCharacters
 * @backingData directive: Uses "starwars.query.AllCharacters" backing data class for pagination
 * @scope directive: This query is scoped to ["starwars"] tenant only
 */
@Resolver
class AllCharactersResolver : QueryResolvers.AllCharacters() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.CharactersConnection? {
        val first = ctx.arguments.first ?: DEFAULT_PAGE_SIZE
        val characters = StarWarsData.characters.take(first)

        // Convert StarWarsData.Character objects to Character objects
        val charactersGrts = characters.map { character ->
            Character.Builder(ctx)
                .id(ctx.globalIDFor(Character.Reflection, character.id))
                .name(character.name)
                .birthYear(character.birthYear)
                .eyeColor(character.eyeColor)
                .gender(character.gender)
                .hairColor(character.hairColor)
                .height(character.height)
                .mass(character.mass?.toDouble())
                .created(character.created.toString())
                .edited(character.edited.toString())
                .build()
        }

        return viaduct.api.grts.CharactersConnection.Builder(ctx)
            .characters(charactersGrts)
            .totalCount(StarWarsData.characters.size)
            .build()
    }
}

/**
 * @resolver directive: Custom field resolution for Query.character
 * @scope directive: This query is scoped to ["starwars"] tenant only
 */
@Resolver
class CharacterResolver : QueryResolvers.Character() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Character? {
        // Get the GlobalID argument - now automatically decoded by framework
        val globalId = ctx.arguments.id

        // Extract the internal ID from the GlobalID
        val stringId = globalId.internalID

        // Find the character in data using the internal ID
        val character = StarWarsData.characters.find { it.id == stringId }

        return if (character != null) {
            // Create the Character GRT with proper GlobalID using globalIdFor
            val result = Character.Builder(ctx)
                .id(ctx.globalIDFor(Character.Reflection, character.id))
                .name(character.name)
                .birthYear(character.birthYear)
                .eyeColor(character.eyeColor)
                .gender(character.gender)
                .hairColor(character.hairColor)
                .height(character.height)
                .mass(character.mass?.toDouble()) // Convert Float to Double
                .created(character.created.toString())
                .edited(character.edited.toString())
                .build()
            result
        } else {
            null
        }
    }
}

/**
 * @resolver directive: Custom field resolution for Query.allFilms
 * @backingData directive: Uses "starwars.query.AllFilms" backing data class for pagination
 * @scope directive: This query is scoped to ["starwars"] tenant only
 */
@Resolver
class AllFilmsResolver : QueryResolvers.AllFilms() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.FilmsConnection? {
        val first = ctx.arguments.first ?: DEFAULT_PAGE_SIZE
        val films = StarWarsData.films.take(first)

        // Convert StarWarsData.Film objects to Film objects
        val filmsGrts = films.map { film ->
            Film.Builder(ctx)
                .id(ctx.globalIDFor(Film.Reflection, film.id))
                .title(film.title)
                .episodeID(film.episodeID)
                .director(film.director)
                .producers(film.producers)
                .releaseDate(film.releaseDate)
                .created(film.created.toString())
                .edited(film.edited.toString())
                .build()
        }

        return viaduct.api.grts.FilmsConnection.Builder(ctx)
            .films(filmsGrts)
            .totalCount(StarWarsData.films.size)
            .build()
    }
}

/**
 * @resolver directive: Custom field resolution for Query.film
 * @scope directive: This query is scoped to ["starwars"] tenant only
 */
@Resolver
class FilmResolver : QueryResolvers.Film() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Film? {
        // Get the GlobalID argument - now automatically decoded by framework
        val globalId = ctx.arguments.id

        // Extract the internal ID from the GlobalID
        val filmId = globalId.internalID

        // Find the film in data using the internal ID
        val film = StarWarsData.films.find { it.id == filmId }

        return if (film != null) {
            val result = Film.Builder(ctx)
                .id(ctx.globalIDFor(Film.Reflection, film.id))
                .title(film.title)
                .episodeID(film.episodeID)
                .director(film.director)
                .producers(film.producers)
                .releaseDate(film.releaseDate)
                .created(film.created.toString())
                .edited(film.edited.toString())
                .build()
            result
        } else {
            null
        }
    }
}

@Resolver
class AllPlanetsResolver : QueryResolvers.AllPlanets() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.PlanetsConnection? {
        val first = ctx.arguments.first ?: DEFAULT_PAGE_SIZE
        val planets = StarWarsData.planets.take(first)
        val planetsGrts = planets.map { planet ->
            Planet.Builder(ctx)
                .id(ctx.globalIDFor(Planet.Reflection, planet.id))
                .name(planet.name)
                .diameter(planet.diameter)
                .rotationPeriod(planet.rotationPeriod)
                .orbitalPeriod(planet.orbitalPeriod)
                .gravity(planet.gravity?.toDouble())
                .population(planet.population?.toDouble())
                .climates(planet.climates)
                .terrains(planet.terrains)
                .surfaceWater(planet.surfaceWater?.toDouble())
                .created(planet.created.toString())
                .edited(planet.edited.toString())
                .build()
        }

        return viaduct.api.grts.PlanetsConnection.Builder(ctx)
            .planets(planetsGrts)
            .totalCount(StarWarsData.planets.size)
            .build()
    }
}

@Resolver
class AllSpeciesResolver : QueryResolvers.AllSpecies() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.SpeciesConnection? {
        val first = ctx.arguments.first ?: DEFAULT_PAGE_SIZE
        val species = StarWarsData.species.take(first)
        val speciesGrts = species.map { speciesItem ->
            Species.Builder(ctx)
                .id(ctx.globalIDFor(Species.Reflection, speciesItem.id))
                .name(speciesItem.name)
                .classification(speciesItem.classification)
                .designation(speciesItem.designation)
                .averageHeight(speciesItem.averageHeight?.toDouble())
                .averageLifespan(speciesItem.averageLifespan)
                .eyeColors(speciesItem.eyeColors)
                .hairColors(speciesItem.hairColors)
                .language(speciesItem.language)
                .created(speciesItem.created.toString())
                .edited(speciesItem.edited.toString())
                .build()
        }

        return viaduct.api.grts.SpeciesConnection.Builder(ctx)
            .species(speciesGrts)
            .totalCount(StarWarsData.species.size)
            .build()
    }
}

@Resolver
class AllStarshipsResolver : QueryResolvers.AllStarships() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.StarshipsConnection? {
        val first = ctx.arguments.first ?: DEFAULT_PAGE_SIZE
        val starships = StarWarsData.starships.take(first)
        val starshipsGrts = starships.map { starship ->
            Starship.Builder(ctx)
                .id(ctx.globalIDFor(Starship.Reflection, starship.id))
                .name(starship.name)
                .model(starship.model)
                .starshipClass(starship.starshipClass)
                .manufacturers(starship.manufacturers)
                .costInCredits(starship.costInCredits?.toDouble())
                .length(starship.length?.toDouble())
                .crew(starship.crew)
                .passengers(starship.passengers)
                .maxAtmospheringSpeed(starship.maxAtmospheringSpeed)
                .hyperdriveRating(starship.hyperdriveRating?.toDouble())
                .MGLT(starship.mglt)
                .cargoCapacity(starship.cargoCapacity?.toDouble())
                .consumables(starship.consumables)
                .created(starship.created.toString())
                .edited(starship.edited.toString())
                .build()
        }

        return viaduct.api.grts.StarshipsConnection.Builder(ctx)
            .starships(starshipsGrts)
            .totalCount(StarWarsData.starships.size)
            .build()
    }
}

@Resolver
class AllVehiclesResolver : QueryResolvers.AllVehicles() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.VehiclesConnection? {
        val first = ctx.arguments.first ?: DEFAULT_PAGE_SIZE
        val vehicles = StarWarsData.vehicles.take(first)
        val vehiclesGrts = vehicles.map { vehicle ->
            Vehicle.Builder(ctx)
                .id(ctx.globalIDFor(Vehicle.Reflection, vehicle.id))
                .name(vehicle.name)
                .model(vehicle.model)
                .vehicleClass(vehicle.vehicleClass)
                .manufacturers(vehicle.manufacturers)
                .costInCredits(vehicle.costInCredits?.toDouble())
                .length(vehicle.length?.toDouble())
                .crew(vehicle.crew)
                .passengers(vehicle.passengers)
                .maxAtmospheringSpeed(vehicle.maxAtmospheringSpeed)
                .cargoCapacity(vehicle.cargoCapacity?.toDouble())
                .consumables(vehicle.consumables)
                .created(vehicle.created.toString())
                .edited(vehicle.edited.toString())
                .build()
        }

        return viaduct.api.grts.VehiclesConnection.Builder(ctx)
            .vehicles(vehiclesGrts)
            .totalCount(StarWarsData.vehicles.size)
            .build()
    }
}

@Resolver
class PlanetResolver : QueryResolvers.Planet() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Planet? {
        // Get the GlobalID argument - now automatically decoded by framework
        val globalId = ctx.arguments.id

        // Extract the internal ID from the GlobalID
        val stringId = globalId.internalID

        // Find the planet in data using the internal ID
        val planet = StarWarsData.planets.find { it.id == stringId }

        return if (planet != null) {
            // Create the Planet GRT with proper GlobalID using globalIdFor
            Planet.Builder(ctx)
                .id(ctx.globalIDFor(Planet.Reflection, planet.id))
                .name(planet.name)
                .diameter(planet.diameter)
                .rotationPeriod(planet.rotationPeriod)
                .orbitalPeriod(planet.orbitalPeriod)
                .gravity(planet.gravity?.toDouble())
                .population(planet.population?.toDouble())
                .climates(planet.climates)
                .terrains(planet.terrains)
                .surfaceWater(planet.surfaceWater?.toDouble())
                .created(planet.created.toString())
                .edited(planet.edited.toString())
                .build()
        } else {
            null
        }
    }
}

@Resolver
class SpeciesResolver : QueryResolvers.Species() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Species? {
        // Get the GlobalID argument - now automatically decoded by framework
        val globalId = ctx.arguments.id

        // Extract the internal ID from the GlobalID
        val stringId = globalId.internalID

        // Find the species in data using the internal ID
        val species = StarWarsData.species.find { it.id == stringId }

        return if (species != null) {
            // Create the Species GRT with proper GlobalID using globalIdFor
            Species.Builder(ctx)
                .id(ctx.globalIDFor(Species.Reflection, species.id))
                .name(species.name)
                .classification(species.classification)
                .designation(species.designation)
                .averageHeight(species.averageHeight?.toDouble())
                .averageLifespan(species.averageLifespan)
                .eyeColors(species.eyeColors)
                .hairColors(species.hairColors)
                .language(species.language)
                .created(species.created.toString())
                .edited(species.edited.toString())
                .build()
        } else {
            null
        }
    }
}

@Resolver
class StarshipResolver : QueryResolvers.Starship() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Starship? {
        // Get the GlobalID argument - now automatically decoded by framework
        val globalId = ctx.arguments.id

        // Extract the internal ID from the GlobalID
        val stringId = globalId.internalID

        // Find the starship in data using the internal ID
        val starship = StarWarsData.starships.find { it.id == stringId }

        return if (starship != null) {
            // Create the Starship GRT with proper GlobalID using globalIdFor
            Starship.Builder(ctx)
                .id(ctx.globalIDFor(Starship.Reflection, starship.id))
                .name(starship.name)
                .model(starship.model)
                .starshipClass(starship.starshipClass)
                .manufacturers(starship.manufacturers)
                .costInCredits(starship.costInCredits?.toDouble())
                .length(starship.length?.toDouble())
                .crew(starship.crew)
                .passengers(starship.passengers)
                .maxAtmospheringSpeed(starship.maxAtmospheringSpeed)
                .hyperdriveRating(starship.hyperdriveRating?.toDouble())
                .MGLT(starship.mglt)
                .cargoCapacity(starship.cargoCapacity?.toDouble())
                .consumables(starship.consumables)
                .created(starship.created.toString())
                .edited(starship.edited.toString())
                .build()
        } else {
            null
        }
    }
}

@Resolver
class VehicleResolver : QueryResolvers.Vehicle() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Vehicle? {
        // Get the GlobalID argument - now automatically decoded by framework
        val globalId = ctx.arguments.id

        // Extract the internal ID from the GlobalID
        val stringId = globalId.internalID

        // Find the vehicle in data using the internal ID
        val vehicle = StarWarsData.vehicles.find { it.id == stringId }

        return if (vehicle != null) {
            // Create the Vehicle GRT with proper GlobalID using globalIdFor
            Vehicle.Builder(ctx)
                .id(ctx.globalIDFor(Vehicle.Reflection, vehicle.id))
                .name(vehicle.name)
                .model(vehicle.model)
                .vehicleClass(vehicle.vehicleClass)
                .manufacturers(vehicle.manufacturers)
                .costInCredits(vehicle.costInCredits?.toDouble())
                .length(vehicle.length?.toDouble())
                .crew(vehicle.crew)
                .passengers(vehicle.passengers)
                .maxAtmospheringSpeed(vehicle.maxAtmospheringSpeed)
                .cargoCapacity(vehicle.cargoCapacity?.toDouble())
                .consumables(vehicle.consumables)
                .created(vehicle.created.toString())
                .edited(vehicle.edited.toString())
                .build()
        } else {
            null
        }
    }
}
