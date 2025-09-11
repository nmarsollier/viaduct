package viaduct.demoapp.starwars.resolvers

import viaduct.api.Resolver
import viaduct.demoapp.starwars.data.StarWarsData
import viaduct.demoapp.starwars.resolverbases.SpeciesResolvers

@Resolver("id")
class SpeciesCulturalNotesResolver : SpeciesResolvers.CulturalNotes() {
    override suspend fun resolve(ctx: Context): String? {
        val speciesGrt = ctx.objectValue
        val speciesId = speciesGrt.getId().internalID
        val species = StarWarsData.species.find { it.id == speciesId }
        return species?.extrasData?.culturalNotes
    }
}

@Resolver("id")
class SpeciesRarityLevelResolver : SpeciesResolvers.RarityLevel() {
    override suspend fun resolve(ctx: Context): String? {
        val speciesGrt = ctx.objectValue
        val speciesId = speciesGrt.getId().internalID
        val species = StarWarsData.species.find { it.id == speciesId }
        return species?.extrasData?.rarityLevel
    }
}

@Resolver("id")
class SpeciesSpecialAbilitiesResolver : SpeciesResolvers.SpecialAbilities() {
    override suspend fun resolve(ctx: Context): List<String?>? {
        val speciesGrt = ctx.objectValue
        val speciesId = speciesGrt.getId().internalID
        val species = StarWarsData.species.find { it.id == speciesId }
        return species?.extrasData?.specialAbilities
    }
}

@Resolver("id")
class SpeciesTechnologicalLevelResolver : SpeciesResolvers.TechnologicalLevel() {
    override suspend fun resolve(ctx: Context): String? {
        val speciesGrt = ctx.objectValue
        val speciesId = speciesGrt.getId().internalID
        val species = StarWarsData.species.find { it.id == speciesId }
        return species?.extrasData?.technologicalLevel
    }
}
