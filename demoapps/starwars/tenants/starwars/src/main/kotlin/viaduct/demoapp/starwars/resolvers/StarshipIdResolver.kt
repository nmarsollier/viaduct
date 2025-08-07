package viaduct.demoapp.starwars.resolvers

import viaduct.api.Resolver
import viaduct.demoapp.starwars.data.StarWarsData
import viaduct.demoapp.starwars.resolverbases.StarshipResolvers

/**
 * Resolvers for Starship type fields
 * Only id field needs a resolver since other fields are handled automatically
 */

@Resolver
class StarshipIdResolver : StarshipResolvers.Id() {
    override suspend fun resolve(ctx: Context): String {
        val starshipId = ctx.objectValue.getId()
        return StarWarsData.starships.find { it.id == starshipId }?.id ?: throw IllegalStateException("Starship not found")
    }
}
