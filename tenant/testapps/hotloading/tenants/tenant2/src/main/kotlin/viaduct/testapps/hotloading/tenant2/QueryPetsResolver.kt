package viaduct.testapps.hotloading.tenant2

import viaduct.api.Resolver
import viaduct.api.grts.Cat
import viaduct.api.grts.Pet
import viaduct.testapps.hotloading.tenant2.resolverbases.QueryResolvers

@Resolver
class QueryPetsResolver : QueryResolvers.Pets() {
    override suspend fun resolve(ctx: Context): List<Pet> {
        return listOf<Pet>(
            Cat.Builder(ctx)
                .name("Fluffy")
                .meow("I'm hungry")
                .build()
        )
    }
}
