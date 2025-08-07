package viaduct.demoapp.starwars.resolvers

import viaduct.api.Resolver
import viaduct.demoapp.starwars.data.StarWarsData
import viaduct.demoapp.starwars.resolverbases.VehicleResolvers

/**
 * Resolvers for Vehicle type fields
 * Only id field needs a resolver since other fields are handled automatically
 */

@Resolver
class VehicleIdResolver : VehicleResolvers.Id() {
    override suspend fun resolve(ctx: Context): String {
        val vehicleId = ctx.objectValue.getId()
        return StarWarsData.vehicles.find { it.id == vehicleId }?.id ?: throw IllegalStateException("Vehicle not found")
    }
}
