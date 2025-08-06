@file:OptIn(ExperimentalCoroutinesApi::class)

package viaduct.arbitrary.graphql

import io.kotest.property.Arb
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.forAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import viaduct.arbitrary.common.Config
import viaduct.arbitrary.common.KotestPropertyBase
import viaduct.arbitrary.common.checkInvariants

class GraphQLNamesTest : KotestPropertyBase() {
    @Test
    fun `Arb_graphQLNames`() =
        runBlockingTest {
            Arb.int(min = 1, max = 1000)
                .forAll { count ->
                    val cfg = Config.default + (SchemaSize to count)
                    val names = Arb.graphQLNames(cfg).bind()

                    val subtotal =
                        names.interfaces.size +
                            names.objects.size +
                            names.inputs.size +
                            names.unions.size +
                            names.scalars.size +
                            names.enums.size +
                            names.directives.size

                    subtotal == names.allNames.size
                }
        }

    @Test
    fun `Arb_graphQLNames -- IncludeCustomScalars`() {
        Arb.of(true, false)
            .flatMap { incl ->
                val cfg = Config.default + (GenCustomScalars to incl)
                Arb.graphQLNames(cfg)
                    .map { names -> incl to names.scalars - builtinScalars.keys }
            }
            .checkInvariants { (incl, scalars), check ->
                if (!incl) {
                    check.isTrue(
                        scalars.isEmpty(),
                        "Found ${scalars.size} scalars when IncludeCustomScalars is $incl"
                    )
                }
            }
    }

    @Test
    fun `graphQLNames plus`() =
        runBlockingTest {
            Arb.pair(
                Arb.graphQLNames(),
                Arb.graphQLNames()
            ).forAll { (a, b) ->
                (a.allNames + b.allNames) == (a + b).allNames
            }
        }

    @Test
    fun `filter`() =
        runBlockingTest {
            Arb.graphQLNames().forAll { names ->
                names.filter { false } == GraphQLNames.empty
            }

            Arb.graphQLNames().forAll { names ->
                names.filter { true } == names
            }
        }
}
