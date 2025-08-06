package viaduct.tenant.codegen.bytecode.config

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import viaduct.graphql.schema.ViaductExtendedSchema
import viaduct.tenant.codegen.bytecode.util.assertKotlinTypeString
import viaduct.tenant.codegen.bytecode.util.field
import viaduct.tenant.codegen.bytecode.util.typedef

class IdTest {
    private fun mkSchema(sdl: String = ""): ViaductExtendedSchema =
        viaduct.graphql.schema.test.mkSchema(
            """
                directive @idOf(type: String!) on FIELD_DEFINITION | INPUT_FIELD_DEFINITION | ARGUMENT_DEFINITION
                interface Node { id: ID! }
                $sdl
            """.trimIndent()
        )

    private val pkg: String = "pkg"

    private fun objectGlobalID(type: String) =
        // "viaduct.api.globalid.GlobalID<pkg.Foo>"
        "${cfg.MODERN_GLOBALID}<$pkg.$type>"

    private fun interfaceOrUnionGlobalID(type: String) =
        // "viaduct.api.globalid.GlobalID<out pkg.Foo>"
        "${cfg.MODERN_GLOBALID}<out $pkg.$type>"

    private fun idOfGlobalID(type: String) =
        // "viaduct.api.globalid.GlobalID<out pkg.Foo>"
        "${cfg.MODERN_GLOBALID}<out $pkg.$type>"

    @BeforeEach
    @AfterEach
    fun setup() {
        cfg.isModern = false
    }

    @Test
    fun `TypeDef_isID`() {
        mkSchema(
            """
            scalar ID
            scalar S
            """.trimIndent()
        ).apply {
            assertTrue(typedef("ID").isID)
            assertFalse(typedef("S").isID)
        }
    }

    @Test
    fun `classic`() {
        val schema = mkSchema(
            """
            type MyNode implements Node { id: ID! }
            type Object {
                f1: ID
                f2: ID!
                f3: [ID]
                f4: [ID!]!
            }
            """.trimIndent()
        )
        schema.field("Node", "id").assertKotlinTypeString("kotlin.String")
        schema.field("Node", "id").assertKotlinTypeString("kotlin.String")
        schema.field("MyNode", "id").assertKotlinTypeString("kotlin.String")
        schema.field("Object", "f1").assertKotlinTypeString("kotlin.String?")
        schema.field("Object", "f2").assertKotlinTypeString("kotlin.String")
        schema.field("Object", "f3")
            .assertKotlinTypeString("kotlin.collections.List<kotlin.String?>?")
        schema.field("Object", "f4")
            .assertKotlinTypeString("kotlin.collections.List<kotlin.String>")
    }

    @Test
    fun `modern -- bare`() {
        cfg.isModern = true
        mkSchema().typedef("ID").assertKotlinTypeString("kotlin.String?")
    }

    @Test
    fun `modern -- Node`() {
        cfg.isModern = true
        mkSchema().field("Node", "id").assertKotlinTypeString(interfaceOrUnionGlobalID("Node"))
    }

    @Test
    fun `modern -- node interface`() {
        cfg.isModern = true
        val schema = mkSchema(
            """
            interface Node2 implements Node {
                id: ID!
                id2: ID!
                id3: ID! @idOf(type: "MyNode")
            }
            """.trimIndent()
        )

        // for interfaces that are-or-implement Node, IDs are untyped unless they use @idOf
        schema.field("Node2", "id").assertKotlinTypeString(interfaceOrUnionGlobalID("Node2"))
        schema.field("Node2", "id2").assertKotlinTypeString("kotlin.String")
        schema.field("Node2", "id3").assertKotlinTypeString(idOfGlobalID("MyNode"))
    }

    @Test
    fun `modern -- node object`() {
        cfg.isModern = true
        val schema = mkSchema(
            """
                type MyNode implements Node {
                    id: ID!
                    id2: ID!
                    id3: ID! @idOf(type:"MyNode")
                }
            """.trimIndent()
        )

        schema.field("MyNode", "id").assertKotlinTypeString(objectGlobalID("MyNode"))
        schema.field("MyNode", "id2").assertKotlinTypeString("kotlin.String")
        schema.field("MyNode", "id3").assertKotlinTypeString(idOfGlobalID("MyNode"))
    }

    @Test
    fun `modern -- nulls and lists`() {
        cfg.isModern = true
        val schema = mkSchema(
            """
                type MyNode implements Node { id: ID! }

                type Object {
                    f1: ID
                    f2: ID!
                    f3: [ID]
                    f4: [ID!]!

                    f5: ID @idOf(type: "MyNode")
                    f6: ID! @idOf(type: "MyNode")
                    f7: [ID] @idOf(type: "MyNode")
                    f8: [ID!]! @idOf(type: "MyNode")

                }
            """.trimIndent()
        )

        schema.field("Object", "f1")
            .assertKotlinTypeString("kotlin.String?")
        schema.field("Object", "f2")
            .assertKotlinTypeString("kotlin.String")
        schema.field("Object", "f3")
            .assertKotlinTypeString("kotlin.collections.List<kotlin.String?>?")
        schema.field("Object", "f4")
            .assertKotlinTypeString("kotlin.collections.List<kotlin.String>")

        schema.field("Object", "f5")
            .assertKotlinTypeString("${idOfGlobalID("MyNode")}?")
        schema.field("Object", "f6")
            .assertKotlinTypeString(idOfGlobalID("MyNode"))
        schema.field("Object", "f7")
            .assertKotlinTypeString("kotlin.collections.List<${idOfGlobalID("MyNode")}?>?")
        schema.field("Object", "f8")
            .assertKotlinTypeString("kotlin.collections.List<${idOfGlobalID("MyNode")}>")
    }

    @Test
    fun `modern -- input`() {
        cfg.isModern = true
        val schema = mkSchema(
            """
                type MyNode implements Node { id: ID! }
                input Input {
                    f1: ID!
                    f2: ID! @idOf(type: "MyNode")
                    f3: ID! = "",
                    f4: ID! = "" @idOf(type: "MyNode")
                }
            """.trimIndent()
        )
        schema.field("Input", "f1").assertKotlinTypeString("kotlin.String")
        schema.field("Input", "f2").assertKotlinTypeString(idOfGlobalID("MyNode"))
        schema.field("Input", "f3").assertKotlinTypeString("kotlin.String")
        schema.field("Input", "f4").assertKotlinTypeString(idOfGlobalID("MyNode"))
    }

    @Test
    fun `modern -- field arguments`() {
        cfg.isModern = true
        val schema = mkSchema(
            """
                type MyNode implements Node { id: ID! }
                type Object {
                    f(
                        a1: ID!,
                        a2: ID! @idOf(type: "MyNode"),
                        a3: ID! = "",
                        a4: ID! = "" @idOf(type: "MyNode"),
                    ): Int
                }
            """.trimIndent()
        )
        val args = schema.field("Object", "f").args.associateBy { it.name }

        args["a1"]!!.assertKotlinTypeString("kotlin.String")
        args["a2"]!!.assertKotlinTypeString(idOfGlobalID("MyNode"))
        args["a3"]!!.assertKotlinTypeString("kotlin.String")
        args["a4"]!!.assertKotlinTypeString(idOfGlobalID("MyNode"))
    }

    @Test
    fun `modern -- globalIDTypeName`() {
        cfg.isModern = true
        val schema = mkSchema(
            """
            interface Node2 implements Node {
                id: ID!
                id2: ID!
                id3: ID! @idOf(type: "MyNode")
            }
            """.trimIndent()
        )

        // for interfaces that are-or-implement Node, IDs are untyped unless they use @idOf
        assertEquals(schema.field("Node2", "id").globalIDTypeName(), "Node2")
        assertEquals(schema.field("Node2", "id2").globalIDTypeName(), null)
        assertEquals(schema.field("Node2", "id3").globalIDTypeName(), "MyNode")
    }
}
