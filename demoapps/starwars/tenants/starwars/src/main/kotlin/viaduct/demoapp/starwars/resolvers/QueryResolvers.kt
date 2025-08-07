package viaduct.demoapp.starwars.resolvers

import org.springframework.stereotype.Component
import viaduct.api.Resolver
import viaduct.demoapp.starwars.data.StarWarsData
import viaduct.demoapp.starwars.resolverbases.QueryResolvers

/**
 * Query resolvers for the Star Wars GraphQL API.
 *
 * The Query type demonstrates the @scope directive which restricts schema access
 * to specific tenants or contexts. All resolvers here are scoped to "starwars".
 */

/**
 * @resolver directive: Custom field resolution for Query.searchPerson
 * @oneOf directive: The search input uses @oneOf directive, ensuring exactly one
 *                  search criteria is provided. This demonstrates input validation
 *                  where only one of byName, byId, or byBirthYear can be specified.
 */
@Component
@Resolver
class SearchPersonResolver : QueryResolvers.SearchPerson() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Person? {
        val search = ctx.arguments.search
        val byName = search.byName
        val byId = search.byId
        val byBirthYear = search.byBirthYear

        val person = when {
            byName != null -> StarWarsData.people.find { it.name.contains(byName, ignoreCase = true) }
            byId != null -> StarWarsData.people.find { it.id == byId }
            byBirthYear != null -> StarWarsData.people.find { it.birthYear == byBirthYear }
            else -> null
        }

        return if (person != null) {
            viaduct.api.grts.Person.Builder(ctx)
                .id(person.id)
                .name(person.name)
                .birthYear(person.birthYear)
                .eyeColor(person.eyeColor)
                .gender(person.gender)
                .hairColor(person.hairColor)
                .height(person.height)
                .mass(person.mass?.toDouble())
                .skinColor(person.skinColor)
                .created(person.created.toString())
                .edited(person.edited.toString())
                .build()
        } else {
            null
        }
    }
}

/**
 * @resolver directive: Custom field resolution for Query.allPeople
 * @backingData directive: Uses "starwars.query.AllPeople" backing data class for pagination
 * @scope directive: This query is scoped to ["starwars"] tenant only
 */
@Component
@Resolver
class AllPeopleResolver : QueryResolvers.AllPeople() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.PeopleConnection? {
        val first = ctx.arguments.first ?: 10
        val people = StarWarsData.people.take(first)

        // Convert StarWarsData.Person objects to viaduct.api.grts.Person objects
        val peopleGrts = people.map { person ->
            viaduct.api.grts.Person.Builder(ctx)
                .id(person.id)
                .name(person.name)
                .birthYear(person.birthYear)
                .eyeColor(person.eyeColor)
                .gender(person.gender)
                .hairColor(person.hairColor)
                .height(person.height)
                .mass(person.mass?.toDouble())
                .skinColor(person.skinColor)
                .created(person.created.toString())
                .edited(person.edited.toString())
                .build()
        }

        return viaduct.api.grts.PeopleConnection.Builder(ctx)
            .people(peopleGrts)
            .totalCount(StarWarsData.people.size)
            .build()
    }
}

/**
 * @resolver directive: Custom field resolution for Query.person
 * @scope directive: This query is scoped to ["starwars"] tenant only
 */
@Component
@Resolver
class PersonResolver : QueryResolvers.Person() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Person? {
        println("DEBUG: PersonResolver.resolve() called!")

        // Check both id and personID arguments as defined in schema
        val id = ctx.arguments.id
        val personID = ctx.arguments.personID
        println("DEBUG: Arguments - id: $id, personID: $personID")

        // Use the actual ID from the arguments instead of hardcoding
        val targetId = id ?: personID
        val person = if (targetId != null) {
            StarWarsData.people.find { it.id == targetId }
        } else {
            null
        }
        println("DEBUG: Found person: ${person?.name} with id: ${person?.id}")

        return if (person != null) {
            val result = viaduct.api.grts.Person.Builder(ctx)
                .id(person.id)
                .name(person.name)
                .birthYear(person.birthYear)
                .eyeColor(person.eyeColor)
                .gender(person.gender)
                .hairColor(person.hairColor)
                .height(person.height)
                .mass(person.mass?.toDouble()) // Convert Float to Double
                .skinColor(person.skinColor)
                .created(person.created.toString())
                .edited(person.edited.toString())
                .build()
            println("DEBUG: Created Person GRT with id: ${person.id}, name: ${person.name}")
            result
        } else {
            println("DEBUG: Person not found, returning null")
            null
        }
    }
}

/**
 * @resolver directive: Custom field resolution for Query.allFilms
 * @backingData directive: Uses "starwars.query.AllFilms" backing data class for pagination
 * @scope directive: This query is scoped to ["starwars"] tenant only
 */
@Component
@Resolver
class AllFilmsResolver : QueryResolvers.AllFilms() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.FilmsConnection? {
        val first = ctx.arguments.first ?: 10
        val films = StarWarsData.films.take(first)

        // Convert StarWarsData.Film objects to viaduct.api.grts.Film objects
        val filmsGrts = films.map { film ->
            viaduct.api.grts.Film.Builder(ctx)
                .id(film.id)
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
@Component
@Resolver
class FilmResolver : QueryResolvers.Film() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Film? {
        println("DEBUG: FilmResolver.resolve() called!")

        // Check both id and filmID arguments as defined in schema
        val id = ctx.arguments.id
        val filmID = ctx.arguments.filmID
        println("DEBUG: Arguments - id: $id, filmID: $filmID")

        val filmId = id ?: filmID
        println("DEBUG: Using filmId: $filmId")

        val film = if (filmId != null) {
            StarWarsData.films.find { it.id == filmId }
        } else {
            null
        }
        println("DEBUG: Found film: ${film?.title} with id: ${film?.id}")

        return if (film != null) {
            val result = viaduct.api.grts.Film.Builder(ctx)
                .id(film.id)
                .title(film.title)
                .episodeID(film.episodeID)
                .director(film.director)
                .producers(film.producers)
                .releaseDate(film.releaseDate)
                .created(film.created.toString())
                .edited(film.edited.toString())
                .build()
            println("DEBUG: Created Film GRT with id: ${film.id}, title: ${film.title}")
            result
        } else {
            println("DEBUG: Film not found, returning null")
            null
        }
    }
}

// Missing query resolvers
@Component
@Resolver
class AllPlanetsResolver : QueryResolvers.AllPlanets() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.PlanetsConnection? {
        return null
    }
}

@Component
@Resolver
class AllSpeciesResolver : QueryResolvers.AllSpecies() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.SpeciesConnection? {
        return null
    }
}

@Component
@Resolver
class AllStarshipsResolver : QueryResolvers.AllStarships() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.StarshipsConnection? {
        return null
    }
}

@Component
@Resolver
class AllVehiclesResolver : QueryResolvers.AllVehicles() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.VehiclesConnection? {
        return null
    }
}

@Component
@Resolver
class PlanetResolver : QueryResolvers.Planet() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Planet? {
        return null
    }
}

@Component
@Resolver
class SpeciesResolver : QueryResolvers.Species() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Species? {
        return null
    }
}

@Component
@Resolver
class StarshipResolver : QueryResolvers.Starship() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Starship? {
        return null
    }
}

@Component
@Resolver
class VehicleResolver : QueryResolvers.Vehicle() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Vehicle? {
        return null
    }
}
