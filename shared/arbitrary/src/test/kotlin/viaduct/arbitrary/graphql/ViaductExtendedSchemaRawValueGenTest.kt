@file:OptIn(ExperimentalCoroutinesApi::class)

package viaduct.arbitrary.graphql

import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import io.kotest.property.forAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import viaduct.arbitrary.common.KotestPropertyBase
import viaduct.arbitrary.graphql.RawValue.Companion.inull
import viaduct.arbitrary.graphql.RawValue.Companion.obj

class ViaductExtendedSchemaRawValueGenTest : KotestPropertyBase() {
    @Test
    fun `non-null recursive output types`() =
        runBlockingTest {
            val schema = mkViaductSchema("type Type { type: Type! }")
            val type = schema.types["Type"]!!.asTypeExpr()

            // expect that even with implicit null weight of 0, values will still be finite
            Arb.rawValueFor(type, cfg = mkConfig(inull = 0.0, maxValueDepth = 1))
                .forAll { v ->
                    v == obj("Type", "type" to BridgeGJToRaw.obj("Type", "type" to inull))
                }
        }

    @Test
    fun `union value gen`() =
        runBlockingTest {
            val schema = mkViaductSchema(
                """
            union Union = Obj
            type Obj { a: Int }
                """.trimIndent()
            )
            val type = schema.types["Union"]!!.asTypeExpr()

            Arb.rawValueFor(type).forAll { v ->
                v is RawObject && v.typename == "Obj"
            }
        }

    @Test
    fun `interface value gen`() =
        runBlockingTest {
            val schema = mkViaductSchema(
                """
            interface Interface { a: Int }
            type Obj implements Interface { a: Int }
                """.trimIndent()
            )
            val type = schema.types["Interface"]!!.asTypeExpr()

            Arb.rawValueFor(type).forAll { v ->
                v is RawObject && v.typename == "Obj"
            }
        }

    @Test
    fun `generates values for arbitrary types`() =
        runBlockingTest {
            val cfg = mkConfig(
                enull = .2,
                inull = .2,
                maxValueDepth = 2,
                schemaSize = 10,
                // this is needed for generating RawObject values for interface types
                genInterfaceStubs = true,
                listValueSize = 3
            )
            Arb.viaductExtendedSchema(cfg)
                .map { it.types.values }
                .checkAll(1000) { defs ->
                    defs.forEach {
                        Arb.rawValueFor(it.asTypeExpr(), cfg).bind()
                    }
                    markSuccess()
                }
        }
}
