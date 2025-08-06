package viaduct.arbitrary.graphql

import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.map
import viaduct.arbitrary.common.Config
import viaduct.graphql.schema.ViaductExtendedSchema
import viaduct.graphql.schema.graphqljava.GJSchema
import viaduct.graphql.schema.graphqljava.ValueConverter

/** Generate an Arb of [ViaductExtendedSchema] from Config */
fun Arb.Companion.viaductExtendedSchema(
    config: Config = Config.default,
    valueConverter: ValueConverter = ValueConverter.default
): Arb<ViaductExtendedSchema> = Arb.graphQLSchema(config).map { schema -> GJSchema.fromSchema(schema, valueConverter) }

/** Generate an arbitrary [ViaductExtendedSchema.TypeExpr] */
fun Arb.Companion.typeExpr(
    config: Config = Config.default,
    valueConverter: ValueConverter = ValueConverter.default
): Arb<ViaductExtendedSchema.TypeExpr> =
    viaductExtendedSchema(config, valueConverter)
        .filter { it.types.values.isNotEmpty() }
        .flatMap { schema ->
            Arb.element(schema.types.values)
                .flatMap {
                    val exprs =
                        when (val type = it) {
                            is ViaductExtendedSchema.Record ->
                                type.fields.map { it.type } + type.asTypeExpr()
                            else -> listOf(type.asTypeExpr())
                        }
                    Arb.element(exprs)
                }
        }
