package viaduct.demoapp.starwars.resolvers

import org.springframework.stereotype.Component
import viaduct.api.Resolver
import viaduct.demoapp.starwars.resolverbases.PersonResolvers

/**
 * Person resolvers - only for complex/computed fields that have @resolver directives.
 * Simple scalar properties (name, birthYear, eyeColor, gender, hairColor, height, mass,
 * skinColor, created, edited) are handled directly by the framework without custom resolvers.
 */

/**
 * Resolves the ID field for a Person - required for Node interface implementation.
 */
@Component
@Resolver
class PersonIdResolver : PersonResolvers.Id() {
    override suspend fun resolve(ctx: Context): String {
        println("DEBUG: PersonIdResolver.resolve() called!")
        // For now, hardcode to \"1\" since we can't use GlobalID to determine which person this is
        val result = "1"
        println("DEBUG: PersonIdResolver returning: $result")
        return result
    }
}

/**
 * @resolver directive: Custom field resolution for Person.homeworld
 * Performs a lookup to find the planet associated with this person
 */
@Component
@Resolver
class PersonHomeworldResolver : PersonResolvers.Homeworld() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Planet? {
        // TODO: Implement homeworld resolution without globalIDFor
        return null
    }
}

/**
 * @resolver directive: Custom field resolution for Person.species
 * Performs a lookup to find the species associated with this person
 */
@Component
@Resolver
class PersonSpeciesResolver : PersonResolvers.Species() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Species? {
        // TODO: Implement species resolution without globalIDFor
        return null
    }
}

/**
 * Demonstrates shorthand fragment syntax - delegates to the name field
 * @resolver("name"): Shorthand fragment syntax that delegates resolution to another field.
 *                   This resolver will automatically fetch the "name" field and return its value.
 */
@Component
@Resolver("fragment _ on Person { name }")
class PersonDisplayNameResolver : PersonResolvers.DisplayName() {
    override suspend fun resolve(ctx: Context): String? {
        return ctx.objectValue.getName()
    }
}

/**
 * Demonstrates full fragment syntax - specifies exactly which fields to fetch
 * @resolver("fragment _ on Person { name birthYear }"): Full fragment syntax that specifies
 *          exactly which fields should be fetched from the Person object. This enables
 *          computed fields that depend on multiple other fields.
 */
@Component
@Resolver(
    """
    fragment _ on Person {
        name
        birthYear
    }
    """
)
class PersonDisplaySummaryResolver : PersonResolvers.DisplaySummary() {
    override suspend fun resolve(ctx: Context): String? {
        val person = ctx.objectValue
        val name = person.getName() ?: "Unknown"
        val birthYear = person.getBirthYear() ?: "Unknown birth year"
        return "$name ($birthYear)"
    }
}

/**
 * Another example of full fragment syntax for complex computed fields
 * @resolver("fragment _ on Person { name eyeColor hairColor }"): Fragment syntax
 *          fetching multiple appearance-related fields to create a description
 */
@Component
@Resolver(
    """
    fragment _ on Person {
        name
        eyeColor  
        hairColor
        skinColor
    }
    """
)
class PersonAppearanceDescriptionResolver : PersonResolvers.AppearanceDescription() {
    override suspend fun resolve(ctx: Context): String? {
        val person = ctx.objectValue
        val name = person.getName() ?: "Someone"
        val eyeColor = person.getEyeColor() ?: "unknown eyes"
        val hairColor = person.getHairColor() ?: "unknown hair"
        val skinColor = person.getSkinColor() ?: "unknown skin"

        return "$name has $eyeColor eyes, $hairColor hair, and $skinColor skin"
    }
}
