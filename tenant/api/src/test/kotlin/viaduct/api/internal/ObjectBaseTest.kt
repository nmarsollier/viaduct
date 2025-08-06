package viaduct.api.internal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.api.ViaductFrameworkException
import viaduct.api.ViaductTenantUsageException
import viaduct.api.mocks.MockInternalContext
import viaduct.api.mocks.executionContext
import viaduct.api.schemautils.SchemaUtils
import viaduct.api.testschema.I1
import viaduct.api.testschema.O1
import viaduct.api.testschema.O2
import viaduct.engine.api.EngineObjectData
import viaduct.engine.api.EngineObjectDataBuilder

@OptIn(ExperimentalCoroutinesApi::class)
class ObjectBaseTest {
    private val gqlSchema = SchemaUtils.getSchema()
    private val internalContext = MockInternalContext.mk(gqlSchema, "viaduct.api.testschema")
    private val executionContext = internalContext.executionContext

    // Use viaduct.api.testschema.E1 for the actual E1 value. This is for a regression test and represents a different version (classic)
    // of the GRT for the same GraphQL enum type.
    private enum class E1 { A }

    @Test
    fun `basic test with builder`() =
        runBlockingTest {
            val o1 =
                O1.Builder(executionContext)
                    .stringField("hello")
                    .objectField(
                        O2.Builder(executionContext)
                            .intField(1)
                            .build()
                    )
                    .build()

            assertEquals("hello", o1.getStringField())
            assertEquals(1, o1.getObjectField()!!.getIntField())
            assertThrows<ViaductTenantUsageException> {
                runBlockingTest {
                    o1.getObjectField()!!.getObjectField()
                }
            }
            assertInstanceOf(EngineObjectData::class.java, o1.engineObjectData.fetch("objectField"))
        }

    @Test
    fun `fetch -- aliased scalar`() =
        runBlockingTest {
            val o1 = O1(
                internalContext,
                EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                    .put("aliasedStringField", "ALIASED")
                    .build()
            )
            assertEquals("ALIASED", o1.getStringField("aliasedStringField"))
        }

    @Test
    fun `fetch -- aliased object field`() =
        runBlockingTest {
            val o1 = O1(
                internalContext,
                EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                    .put(
                        "aliasedObjectField",
                        EngineObjectDataBuilder.from(gqlSchema.getObjectType("O2"))
                            .put("aliasedIntField", 42)
                            .build()
                    )
                    .build()
            )
            assertEquals(42, o1.getObjectField("aliasedObjectField")?.getIntField("aliasedIntField"))
        }

    @Test
    fun `fetch -- aliased list of objects`() =
        runBlockingTest {
            val o1 = O1(
                internalContext,
                EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                    .put(
                        "aliasedListField",
                        listOf(
                            listOf(
                                EngineObjectDataBuilder.from(gqlSchema.getObjectType("O2"))
                                    .put("intField", 42)
                                    .build()
                            )
                        )
                    )
                    .build()
            )
            assertEquals(42, o1.getListField("aliasedListField")?.get(0)?.get(0)?.getIntField())
        }

    @Test
    fun `test list of object with builder`() =
        runBlockingTest {
            val o1 =
                O1.Builder(executionContext)
                    .listField(
                        listOf(
                            null,
                            listOf(
                                O2.Builder(executionContext)
                                    .intField(5)
                                    .build(),
                                null
                            )
                        )
                    )
                    .build()

            val listField = o1.getListField()!!
            assertEquals(null, listField[0])
            val innerList = listField[1]!!
            assertEquals(5, innerList[0]!!.getIntField())
            assertEquals(null, innerList[1])
        }

    @Test
    fun `test wrap scalar and object`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put("stringField", "hi")
                        .put(
                            "objectField",
                            EngineObjectDataBuilder.from(gqlSchema.getObjectType("O2"))
                                .put("intField", 1)
                                .build()
                        )
                        .build()
                )
            assertEquals("hi", o1.getStringField())
            assertEquals(1, o1.getObjectField()!!.getIntField())
        }

    @Test
    fun `test wrap scalar - wrong type string`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put("stringField", 1)
                        .build()
                )
            val exception = assertThrows<ViaductFrameworkException> {
                runBlockingTest {
                    o1.getStringField()
                }
            }
            assertTrue(exception.message!!.contains("O1.stringField"))
            assertEquals("Expected a String input, but it was a 'Integer'", exception.cause!!.message)
        }

    @Test
    fun `test wrap scalar - wrong type int`() =
        runBlockingTest {
            val o2 =
                O2(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O2"))
                        .put("intField", "hi")
                        .build()
                )
            val exception = assertThrows<ViaductFrameworkException> {
                runBlockingTest {
                    o2.getIntField()
                }
            }
            assertTrue(exception.message!!.contains("O2.intField"))
            assertEquals("Expected a value that can be converted to type 'Int' but it was a 'String'", exception.cause!!.message)
        }

    @Test
    fun `test wrap enum`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put("enumField", "A")
                        .build()
                )
            assertEquals(viaduct.api.testschema.E1.A, o1.getEnumField())
        }

    @Test
    fun `test wrap enum - different enum type`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put("enumField", E1.A)
                        .build()
                )
            assertEquals(viaduct.api.testschema.E1.A, o1.getEnumField())
        }

    @Test
    fun `test wrap enum - wrong value`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put("enumField", 1)
                        .build()
                )
            val exception = assertThrows<ViaductFrameworkException> {
                runBlockingTest {
                    o1.getEnumField()
                }
            }
            assertTrue(exception.message!!.contains("O1.enumField"))
            assertTrue(exception.cause!!.message!!.contains("No enum constant"))
        }

    @Test
    fun `test wrap enum - wrong type`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put(
                            "enumField",
                            EngineObjectDataBuilder.from(gqlSchema.getObjectType("O2"))
                                .put("intField", 1)
                                .build()
                        )
                        .build()
                )
            val exception = assertThrows<ViaductFrameworkException> {
                runBlockingTest {
                    o1.getEnumField()
                }
            }
            assertTrue(exception.message!!.contains("O1.enumField"))
            assertTrue(exception.cause!!.message!!.contains("No enum constant"))
        }

    @Test
    fun `test wrap interface`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put(
                            "interfaceField",
                            EngineObjectDataBuilder.from(gqlSchema.getObjectType("I1"))
                                .put("commonField", "from I1")
                                .build()
                        )
                        .build()
                )
            assertEquals("from I1", (o1.getInterfaceField() as I1).getCommonField())
        }

    @Test
    fun `test wrap interface - wrong object type`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put(
                            "interfaceField",
                            EngineObjectDataBuilder.from(gqlSchema.getObjectType("O2"))
                                .put("commonField", "from I1")
                                .build()
                        )
                        .build()
                )
            val exception = assertThrows<ViaductFrameworkException> {
                runBlockingTest {
                    o1.getInterfaceField()
                }
            }
            assertTrue(exception.message!!.contains("O1.interfaceField"))
            assertEquals("Expected value to be a subtype of I0, got O2", exception.cause!!.message)
        }

    @Test
    fun `test wrap interface - wrong type not EngineObjectData`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put("interfaceField", "hi")
                        .build()
                )
            val exception = assertThrows<ViaductFrameworkException> {
                runBlockingTest {
                    o1.getInterfaceField()
                }
            }
            assertTrue(exception.message!!.contains("O1.interfaceField"))
            assertEquals("Expected value to be an instance of EngineObjectData, got hi", exception.cause!!.message)
        }

    @Test
    fun `test wrap object - wrong type`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put(
                            "objectField",
                            EngineObjectDataBuilder.from(gqlSchema.getObjectType("I1"))
                                .put("commonField", "from I1")
                                .build()
                        )
                        .build()
                )
            val exception = assertThrows<ViaductFrameworkException> {
                runBlockingTest {
                    o1.getObjectField()
                }
            }
            assertTrue(exception.message!!.contains("O1.objectField"))
            assertEquals("Expected value with GraphQL type O2, got I1", exception.cause!!.message)
        }

    @Test
    fun `test wrap list - wrong base type`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put(
                            "listField",
                            listOf(
                                listOf(
                                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("I1"))
                                        .put("commonField", "from I1")
                                        .build()
                                )
                            )
                        )
                        .build()
                )
            val exception = assertThrows<ViaductFrameworkException> {
                runBlockingTest {
                    o1.getListField()
                }
            }
            assertTrue(exception.message!!.contains("O1.listField"))
            assertEquals("Expected value with GraphQL type O2, got I1", exception.cause!!.message)
        }

    @Test
    fun `test wrap list - wrong type`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put(
                            "listField",
                            listOf(
                                EngineObjectDataBuilder.from(gqlSchema.getObjectType("I1"))
                                    .put("commonField", "from I1")
                                    .build()
                            )
                        )
                        .build()
                )
            val exception = assertThrows<ViaductFrameworkException> {
                runBlockingTest {
                    o1.getListField()
                }
            }
            assertTrue(exception.message!!.contains("O1.listField"))
            assertTrue(exception.cause!!.message!!.contains("Got non-list value"))
        }

    @Test
    fun `test wrap list - wrong with null`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put(
                            "listFieldNonNullBaseType",
                            listOf(
                                null
                            )
                        )
                        .build()
                )
            val exception = assertThrows<ViaductFrameworkException> {
                runBlockingTest {
                    o1.getListFieldNonNullBaseType()
                }
            }
            assertTrue(exception.message!!.contains("O1.listFieldNonNullBaseType"))
            assertEquals("Got null value for non-null type [O2!]!", exception.cause!!.message)
        }

    @Test
    fun `test wrap list - wrong with null base type`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put(
                            "listFieldNonNullBaseType",
                            listOf(
                                listOf(null)
                            )
                        )
                        .build()
                )
            val exception = assertThrows<ViaductFrameworkException> {
                runBlockingTest {
                    o1.getListFieldNonNullBaseType()
                }
            }
            assertTrue(exception.message!!.contains("O1.listFieldNonNullBaseType"))
            assertEquals("Got null value for non-null type O2!", exception.cause!!.message)
        }

    @Test
    fun `test wrapping - framework error null value`() =
        runBlockingTest {
            val o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put(
                            "objectField",
                            EngineObjectDataBuilder.from(gqlSchema.getObjectType("O2"))
                                .put("intField", null)
                                .build()
                        )
                        .build()
                )
            val objectField = o1.getObjectField()!!
            val exception = assertThrows<ViaductFrameworkException> {
                runBlockingTest {
                    objectField.getIntField()
                }
            }
            assertEquals("Got null value for non-null type Int!", exception.cause!!.message)
        }

    @Test
    fun `test unwrapping - framework errors`() =
        runBlockingTest {
            val builder = BuggyBuilder()
            val e1 = assertThrows<ViaductFrameworkException> { builder.intField(null) }
            assertEquals("Got null builder value for non-null type Int!", e1.cause!!.message)
            val e2 = assertThrows<ViaductFrameworkException> { builder.objectField(4) }
            assertEquals("Expected ObjectBase or EngineObjectData for builder value, got 4", e2.cause!!.message)
        }

    @Test
    fun `test wrap - backing data`() =
        runBlockingTest {
            var o2 =
                O2(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O2"))
                        .put("backingDataField", "abc")
                        .build()
                )
            assertEquals("abc", o2.get("backingDataField", String::class))

            o2 =
                O2(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O2"))
                        .put("backingDataField", "abc")
                        .build()
                )
            val exception = assertThrows<IllegalArgumentException> {
                o2.get(
                    "backingDataField",
                    Int::class
                )
            }
            assertTrue(exception.message!!.contains("Expected backing data value to be of type Int, got String"))
        }

    @Test
    fun `test wrap - list backing data`() =
        runBlockingTest {
            var o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put("backingDataList", listOf(1))
                        .build()
                )
            assertEquals(listOf(1), o1.get("backingDataList", Int::class))

            o1 =
                O1(
                    internalContext,
                    EngineObjectDataBuilder.from(gqlSchema.getObjectType("O1"))
                        .put("backingDataList", listOf(1))
                        .build()
                )
            val exception = assertThrows<IllegalArgumentException> { o1.get("backingDataList", String::class) }
            assertTrue(exception.message!!.contains("Expected backing data value to be of type String, got Int"))
        }

    inner class BuggyBuilder : ObjectBase.Builder<O2>(internalContext, gqlSchema.getObjectType("O2")) {
        fun intField(v: Any?): BuggyBuilder {
            putInternal("intField", v)
            return this
        }

        fun objectField(v: Any?): BuggyBuilder {
            putInternal("objectField", v)
            return this
        }

        override fun build() = O2(context, buildEngineObjectData())
    }
}
