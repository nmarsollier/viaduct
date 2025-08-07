package viaduct.demoapp.starwars.resolvers

import viaduct.api.Resolver
import viaduct.demoapp.starwars.data.StarWarsData
import viaduct.demoapp.starwars.resolverbases.SpeciesResolvers

/**
 * Resolvers for Species type fields
 * Only id field needs a resolver since other fields are handled automatically
 */

@Resolver
class SpeciesIdResolver : SpeciesResolvers.Id() {
    override suspend fun resolve(ctx: Context): String {
        val speciesId = ctx.objectValue.getId()
        return StarWarsData.species.find { it.id == speciesId }?.id ?: throw IllegalStateException("Species not found")
    }
}
