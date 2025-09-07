package viaduct.demoapp.starwars

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import viaduct.api.grts.Query_AllStarships_Arguments
import viaduct.api.grts.Query_Starship_Arguments
import viaduct.demoapp.starwars.data.StarshipsData
import viaduct.demoapp.starwars.resolvers.AllStarshipsResolver
import viaduct.demoapp.starwars.resolvers.StarshipResolver
import viaduct.engine.api.ViaductSchema
import viaduct.engine.runtime.execution.DefaultCoroutineInterop
import viaduct.service.runtime.ViaductSchemaRegistryBuilder
import viaduct.tenant.testing.DefaultAbstractResolverTestBase

@OptIn(ExperimentalCoroutinesApi::class)
class StarshipResolversUnitTests : DefaultAbstractResolverTestBase() {
    override fun getSchema(): ViaductSchema =
        ViaductSchemaRegistryBuilder()
            .withFullSchemaFromResources("viaduct.demoapp.starwars", ".*\\.graphqls")
            .build(DefaultCoroutineInterop)
            .getFullSchema()

    private fun queryObj() = viaduct.api.grts.Query.Builder(context).build()

    @Test
    fun `AllStarshipsResolver returns default page size when limit is not provided`() =
        runBlockingTest {
            val resolver = AllStarshipsResolver()
            val args = Query_AllStarships_Arguments.Builder(context).build()

            val result = runFieldResolver(
                resolver = resolver,
                objectValue = queryObj(),
                queryValue = queryObj(),
                arguments = args
            )

            assertNotNull(result)
            assertEquals(2, result!!.size)
        }

    @Test
    fun `AllStarshipsResolver respects custom limit and maps fields`() =
        runBlockingTest {
            val resolver = AllStarshipsResolver()
            val limit = 2
            val args = Query_AllStarships_Arguments.Builder(context).limit(limit).build()

            val result = runFieldResolver(
                resolver = resolver,
                objectValue = queryObj(),
                queryValue = queryObj(),
                arguments = args
            )

            assertNotNull(result)
            assertEquals(2, result!!.size)

            val grt = result.first()!!
            assertEquals("Millennium Falcon", grt.getName())
        }

    @Test
    fun `StarshipResolver returns the starship for a valid id`() =
        runBlockingTest {
            val resolver = StarshipResolver()
            val src = StarshipsData.starships.firstOrNull() ?: error("No starships in StarshipsData")

            val args = Query_Starship_Arguments.Builder(context)
                .id("1")
                .build()

            val result = runFieldResolver(
                resolver = resolver,
                objectValue = queryObj(),
                queryValue = queryObj(),
                arguments = args
            )

            assertNotNull(result)
            val grt = result!!
            assertEquals("Millennium Falcon", grt.getName())
        }

    @Test
    fun `StarshipResolver returns null when id does not exist`() =
        runBlockingTest {
            val resolver = StarshipResolver()

            val args = Query_Starship_Arguments.Builder(context)
                .id("nonexistent-id")
                .build()

            val result = runFieldResolver(
                resolver = resolver,
                objectValue = queryObj(),
                queryValue = queryObj(),
                arguments = args
            )

            assertNull(result)
        }
}
