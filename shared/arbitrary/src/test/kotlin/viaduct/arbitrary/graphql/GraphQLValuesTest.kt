@file:OptIn(ExperimentalCoroutinesApi::class)

package viaduct.arbitrary.graphql

import graphql.schema.GraphQLInputType
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.of
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import viaduct.arbitrary.common.Config
import viaduct.graphql.schema.ViaductExtendedSchema

class GraphQLValuesTest {
    private object ToStringMapper : ValueMapper<ViaductExtendedSchema.TypeExpr, RawValue, String> {
        override fun invoke(
            type: ViaductExtendedSchema.TypeExpr,
            value: RawValue
        ): String = "$type:$value"
    }

    val cfg = Config.default + (GenInterfaceStubsIfNeeded to true) +
        (SchemaSize to 10) +
        (ObjectTypeSize to 1..2) +
        (ListValueSize to 0..1) +
        (MaxValueDepth to 1)

    private val graphQLTypesWithInputs: Arb<GraphQLTypes> =
        Arb.graphQLTypes(cfg).filter { it.allInputs.isNotEmpty() }

    private val GraphQLTypes.allInputs: Collection<GraphQLInputType>
        get() = inputs.values + enums.values + scalars.values

    @Test
    fun `graphQLValueFor -- type and typeresolver`() =
        runBlockingTest {
            graphQLTypesWithInputs.flatMap { types ->
                Arb.of(types.allInputs).flatMap { type ->
                    // The ".Companion" notation is not needed, though it gives a hint to the code coverage scanner
                    // that a function is covered.
                    Arb.Companion.graphQLValueFor(
                        type,
                        TypeReferenceResolver.fromTypes(types),
                        cfg
                    )
                }
            }.assertNoErrors()
        }

    @Test
    fun `graphQLValueFor -- type and types`() =
        runBlockingTest {
            graphQLTypesWithInputs.flatMap { types ->
                Arb.of(types.allInputs).flatMap { type ->
                    Arb.Companion.graphQLValueFor(type, types, cfg)
                }
            }.assertNoErrors()
        }

    @Test
    fun `rawValueFor -- type and resolver`() =
        runBlockingTest {
            graphQLTypesWithInputs.flatMap { types ->
                Arb.of(types.allInputs).flatMap { type ->
                    Arb.Companion.rawValueFor(
                        type,
                        TypeReferenceResolver.fromTypes(types),
                        cfg
                    )
                }
            }.assertNoErrors()
        }

    @Test
    fun `rawValueFor -- type and types`() =
        runBlockingTest {
            graphQLTypesWithInputs.flatMap { types ->
                Arb.of(types.allInputs).flatMap { type ->
                    Arb.Companion.rawValueFor(type, types, cfg)
                }
            }.assertNoErrors()
        }

    @Test
    fun `rawValueFor -- typedef`() =
        runBlockingTest {
            Arb.typeExpr(cfg).flatMap { type ->
                Arb.Companion.rawValueFor(type.baseTypeDef, cfg)
            }.assertNoErrors()
        }

    @Test
    fun `rawValueFor -- bridge typeexpr`() =
        runBlockingTest {
            Arb.typeExpr(cfg).flatMap { type ->
                Arb.Companion.rawValueFor(type, cfg)
            }.assertNoErrors()
        }

    @Test
    fun `mappedValueFor -- mapped bridge typedef `() =
        runBlockingTest {
            Arb.typeExpr(cfg).flatMap { type ->
                Arb.Companion.mappedValueFor(type.baseTypeDef, ToStringMapper, cfg)
            }.assertNoErrors()
        }

    @Test
    fun `mappedValueFor -- mapped bridge typexpr `() =
        runBlockingTest {
            Arb.typeExpr(cfg).flatMap { expr ->
                Arb.Companion.mappedValueFor(expr, ToStringMapper, cfg)
            }.assertNoErrors()
        }
}
