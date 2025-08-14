package viaduct.demoapp.starwars.resolvers

import viaduct.api.Resolver
import viaduct.demoapp.starwars.resolverbases.CharacterResolvers

/**
 * Character resolvers - only for complex/computed fields that have @resolver directives.
 * Simple scalar properties (name, birthYear, eyeColor, gender, hairColor, height, mass,
 * created, edited) are handled directly by the framework without custom resolvers.
 */

/**
 * @resolver directive: Custom field resolution for Character.homeworld
 * Performs a lookup to find the planet associated with this character
 */
@Resolver
class CharacterHomeworldResolver : CharacterResolvers.Homeworld() {
    override suspend fun resolve(ctx: Context): viaduct.api.grts.Planet? {
        // TODO: Implement homeworld resolution without globalIDFor
        return null
    }
}

/**
 * @resolver directive: Custom field resolution for Character.species
 * Performs a lookup to find the species associated with this character
 */
@Resolver
class CharacterSpeciesResolver : CharacterResolvers.Species() {
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
@Resolver("name")
class CharacterDisplayNameResolver : CharacterResolvers.DisplayName() {
    override suspend fun resolve(ctx: Context): String? {
        return ctx.objectValue.getName()
    }
}

/**
 * Demonstrates shorthand fragment syntax - specifies exactly which fields to fetch
 * @resolver("fragment _ on Character { name birthYear }"): Full fragment syntax that specifies
 *          exactly which fields should be fetched from the Character object. This enables
 *          computed fields that depend on multiple other fields.
 */
@Resolver(
    """
        name
        birthYear
    """
)
class CharacterDisplaySummaryResolver : CharacterResolvers.DisplaySummary() {
    override suspend fun resolve(ctx: Context): String? {
        val character = ctx.objectValue
        val name = character.getName() ?: "Unknown"
        val birthYear = character.getBirthYear() ?: "Unknown birth year"
        return "$name ($birthYear)"
    }
}

/**
 * Example of full fragment syntax for complex computed fields
 * @resolver("fragment _ on Character { name eyeColor hairColor }"): Fragment syntax
 *          fetching multiple appearance-related fields to create a description
 */
@Resolver(
    """
    fragment _ on Character {
        name
        eyeColor
        hairColor
    }
    """
)
class CharacterAppearanceDescriptionResolver : CharacterResolvers.AppearanceDescription() {
    override suspend fun resolve(ctx: Context): String? {
        val character = ctx.objectValue
        val name = character.getName() ?: "Someone"
        val eyeColor = character.getEyeColor() ?: "unknown eyes"
        val hairColor = character.getHairColor() ?: "unknown hair"
        return "$name has $eyeColor eyes and $hairColor hair"
    }
}
