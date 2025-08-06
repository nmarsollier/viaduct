package viaduct.tenant.codegen.bytecode.config

import java.io.File
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import viaduct.graphql.schema.ViaductExtendedSchema
import viaduct.graphql.schema.graphqljava.GJSchema
import viaduct.graphql.schema.test.mkSchema
import viaduct.tenant.codegen.bytecode.util.assertKotlinTypeString
import viaduct.tenant.codegen.bytecode.util.field
import viaduct.tenant.codegen.bytecode.util.typedef

class ViaductSchemaExtensionsTest {
    private fun mkSchema(
        schemaText: String,
        schemaFilePath: String,
        repoRoot: File
    ): ViaductExtendedSchema {
        val schemaFile = repoRoot.resolve("$schemaFilePath")
        schemaFile.parentFile.mkdirs()
        schemaFile.createNewFile()
        schemaFile.writeText(schemaText)
        return GJSchema.fromFiles(listOf(schemaFile))
    }

    @BeforeEach
    @AfterEach
    fun setup() {
        cfg.isModern = false
    }

    @Test
    fun `isNode -- object`() {
        assertFalse(mkSchema("type Obj { empty: Int }").typedef("Obj").isNode)
        assertFalse(mkSchema("type Node { empty: Int }").typedef("Node").isNode)

        mkSchema(
            """
            interface I { empty: Int }
            type O implements I { empty: Int }
            """.trimIndent()
        ).apply {
            assertFalse(typedef("O").isNode)
        }

        mkSchema(
            """
                interface Node { empty: Int }
                type O implements Node { empty: Int }
            """.trimIndent()
        ).apply {
            assertTrue(typedef("O").isNode)
        }
    }

    @Test
    fun `isNode -- scalar`() {
        assertFalse(mkSchema("scalar Scalar").typedef("Scalar").isNode)
        assertFalse(mkSchema("scalar Node").typedef("Node").isNode)
    }

    @Test
    fun `isNode -- enum`() {
        assertFalse(mkSchema("enum E { empty }").typedef("E").isNode)
        assertFalse(mkSchema("enum Node { empty }").typedef("Node").isNode)
    }

    @Test
    fun `isNode -- interface`() {
        assertFalse(mkSchema("interface I { empty: Int }").typedef("I").isNode)

        mkSchema(
            """
                interface Super { empty: Int }
                interface I implements Super { empty: Int }
            """.trimIndent()
        ).apply {
            assertFalse(typedef("I").isNode)
        }

        assertTrue(
            mkSchema("interface Node { empty: Int }").typedef("Node").isNode
        )

        mkSchema(
            """
                interface Node { empty: Int }
                interface I implements Node { empty: Int }
            """.trimIndent()
        ).apply {
            assertTrue(typedef("I").isNode)
        }
    }

    @Test
    fun `isNode -- input`() {
        assertFalse(
            mkSchema("input I { empty: Int }").typedef("I").isNode
        )
    }

    @Test
    fun `isConnection -- object`() {
        assertFalse(
            mkSchema("type O { empty: Int }").typedef("O").isConnection
        )
        assertFalse(
            mkSchema("type PagedConnection { empty: Int }").typedef("PagedConnection").isConnection
        )

        mkSchema(
            """
            interface Super { empty: Int }
            interface O implements Super { empty: Int }
            """.trimIndent()
        ).apply {
            assertFalse(typedef("O").isConnection)
        }

        mkSchema(
            """
            interface PagedConnection { empty: Int }
            interface O implements PagedConnection { empty: Int }
            """.trimIndent()
        ).apply {
            assertTrue(typedef("O").isConnection)
        }
    }

    @Test
    fun `isConnection -- scalar`() {
        assertFalse(mkSchema("scalar S").typedef("S").isConnection)
        assertFalse(
            mkSchema("scalar PagedConnection").typedef("PagedConnection").isConnection
        )
    }

    @Test
    fun `isConnection -- enum`() {
        assertFalse(mkSchema("enum E").typedef("E").isConnection)
        assertFalse(
            mkSchema("enum PagedConnection").typedef("PagedConnection").isConnection
        )
    }

    @Test
    fun `isConnection -- interface`() {
        assertFalse(
            mkSchema("interface I { empty: Int }").typedef("I").isConnection
        )

        mkSchema(
            """
                interface Super { empty: Int }
                interface I implements Super { empty: Int }
            """.trimIndent()
        ).apply {
            assertFalse(typedef("I").isConnection)
        }

        assertTrue(
            mkSchema("interface PagedConnection { empty: Int }").typedef("PagedConnection").isConnection
        )

        mkSchema(
            """
            interface PagedConnection { empty: Int }
            interface I implements PagedConnection { empty: Int }
            """.trimIndent()
        ).apply {
            assertTrue(typedef("I").isConnection)
        }
    }

    @Test
    fun `isConnection -- input`() {
        assertFalse(mkSchema("input I { empty: Int }").typedef("I").isConnection)
    }

    @Test
    fun `hasReflectedType`() {
        mkSchema(
            """
                type Obj { empty: Int }
                input Inp { empty: Int }
                interface Iface { empty: Int }
                scalar Scalar
                union Union = Obj
                enum Enum { Value }
            """.trimIndent()
        ).apply {
            assertTrue(typedef("Obj").hasReflectedType)
            assertTrue(typedef("Inp").hasReflectedType)
            assertTrue(typedef("Iface").hasReflectedType)
            assertFalse(typedef("Scalar").hasReflectedType)
            assertTrue(typedef("Union").hasReflectedType)
            assertTrue(typedef("Enum").hasReflectedType)
        }
    }

    @Test
    fun `kotlinTypeString -- lists and nulls`() {
        // lists and nulls
        mkSchema(
            """
                type Obj {
                    f1: Int
                    f2: Int!
                    f3: [Int]
                    f4: [Int!]
                    f5: [Int!]!
                }
            """.trimIndent()
        ).apply {
            typedef("Obj").assertKotlinTypeString("pkg.Obj?")
            field("Obj", "f1").assertKotlinTypeString("kotlin.Int?")
            field("Obj", "f2").assertKotlinTypeString("kotlin.Int")
            field("Obj", "f3").assertKotlinTypeString("kotlin.collections.List<kotlin.Int?>?")
            field("Obj", "f4").assertKotlinTypeString("kotlin.collections.List<kotlin.Int>?")
            field("Obj", "f5").assertKotlinTypeString("kotlin.collections.List<kotlin.Int>")
        }
    }

    @Test
    fun `kotlinTypeString -- scalars`() {
        mkSchema(
            """
                scalar JSON
                scalar Date
                scalar DateTime
                scalar Time
                scalar Byte
            """.trimIndent()
        ).apply {
            typedef("Boolean").assertKotlinTypeString("kotlin.Boolean?")
            typedef("Byte").assertKotlinTypeString("kotlin.Byte?")
            typedef("Date").assertKotlinTypeString("java.time.LocalDate?")
            typedef("DateTime").assertKotlinTypeString("java.time.Instant?")
            typedef("Float").assertKotlinTypeString("kotlin.Double?")
            typedef("ID").assertKotlinTypeString("kotlin.String?")
            typedef("Int").assertKotlinTypeString("kotlin.Int?")
            typedef("JSON").assertKotlinTypeString("kotlin.Any?")
            typedef("Long").assertKotlinTypeString("kotlin.Long?")
            typedef("Short").assertKotlinTypeString("kotlin.Short?")
            typedef("String").assertKotlinTypeString("kotlin.String?")
            typedef("Time").assertKotlinTypeString("java.time.OffsetTime?")
        }
    }
}
