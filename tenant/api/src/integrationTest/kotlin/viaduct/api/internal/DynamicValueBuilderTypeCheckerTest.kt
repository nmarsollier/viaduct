package viaduct.api.internal

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.api.ViaductTenantUsageException
import viaduct.api.mocks.MockGlobalID
import viaduct.api.mocks.MockInternalContext
import viaduct.api.mocks.executionContext
import viaduct.api.schemautils.SchemaUtils
import viaduct.api.testschema.E1
import viaduct.api.testschema.O1
import viaduct.api.testschema.O2
import viaduct.api.testschema.TestUser

class DynamicValueBuilderTypeCheckerTest {
    private val graphqlSchema = SchemaUtils.getSchema()
    private val context = MockInternalContext.mk(graphqlSchema, "viaduct.api.testschema")
    private val checker = DynamicValueBuilderTypeChecker(context)
    private val o1Type = graphqlSchema.schema.getObjectType(O1.Reflection.name)
    private val o2Type = graphqlSchema.schema.getObjectType(O2.Reflection.name)
    private val userType = graphqlSchema.schema.getObjectType("TestUser")
    private val testType = graphqlSchema.schema.getObjectType("TestType")

    @Test
    fun testBasicTypeCheck() {
        val stringField = o1Type.getField("stringField")
        val context = DynamicValueBuilderTypeChecker.FieldContext(stringField, o1Type)
        checker.checkType(stringField.type, null, context)
        checker.checkType(stringField.type, "abc", context)
    }

    @Test
    fun testNonNullField() {
        val intField = o2Type.getField("intField")
        val context = DynamicValueBuilderTypeChecker.FieldContext(intField, o2Type)
        val e = assertThrows<IllegalArgumentException> {
            checker.checkType(intField.type, null, context)
        }
        assertEquals("Got null builder value for non-null type Int! for field intField", e.message)
    }

    @Test
    fun testScalarType() {
        val dateTimeField = o2Type.getField("dateTimeField")
        val context = DynamicValueBuilderTypeChecker.FieldContext(dateTimeField, o2Type)
        checker.checkType(dateTimeField.type, null, context)
        checker.checkType(dateTimeField.type, Instant.now(), context)

        val e = assertThrows<IllegalArgumentException> {
            checker.checkType(dateTimeField.type, 123, context)
        }
        assertEquals("Expected value of type Instant for field dateTimeField, got Int", e.message)
    }

    @Test
    fun testEnumType() {
        val enumField = o1Type.getField("enumField")
        val context = DynamicValueBuilderTypeChecker.FieldContext(enumField, o1Type)
        checker.checkType(enumField.type, null, context)
        checker.checkType(enumField.type, E1.A, context)

        val e = assertThrows<IllegalArgumentException> {
            checker.checkType(enumField.type, "A", context)
        }
        assertEquals("Expected value of type E1 for field enumField, got String", e.message)
    }

    @Test
    fun testNullableListType() {
        val listField = o1Type.getField("listField")
        val context = DynamicValueBuilderTypeChecker.FieldContext(listField, o1Type)
        checker.checkType(listField.type, null, context)
        checker.checkType(listField.type, listOf(null), context)
        checker.checkType(listField.type, listOf(listOf(null)), context)

        var e = assertThrows<IllegalArgumentException> {
            checker.checkType(listField.type, 10, context)
        }
        assertEquals("Got non-list builder value 10 for list type for field listField", e.message)
        e = assertThrows<IllegalArgumentException> {
            checker.checkType(listField.type, listOf(10), context)
        }
        assertEquals("Got non-list builder value 10 for list type for field listField", e.message)
    }

    @Test
    fun testNonNullableListType() {
        val listField = o1Type.getField("listFieldNonNullBaseType")
        val o2 = O2.Builder(context.executionContext).intField(123).build()
        val context = DynamicValueBuilderTypeChecker.FieldContext(listField, o1Type)
        checker.checkType(listField.type, listOf(listOf(o2)), context)

        var e = assertThrows<IllegalArgumentException> {
            checker.checkType(listField.type, listOf(null), context)
        }
        assertEquals("Got null builder value for non-null type [O2!]! for field listFieldNonNullBaseType", e.message)
        e = assertThrows<IllegalArgumentException> {
            checker.checkType(listField.type, listOf(listOf(null)), context)
        }
        assertEquals("Got null builder value for non-null type O2! for field listFieldNonNullBaseType", e.message)
    }

    @Test
    fun testObjectType() {
        val objectField = o1Type.getField("objectField")
        val o2 = O2.Builder(context.executionContext).intField(123).build()
        val context = DynamicValueBuilderTypeChecker.FieldContext(objectField, o1Type)
        checker.checkType(objectField.type, null, context)
        checker.checkType(objectField.type, o2, context)

        val e = assertThrows<IllegalArgumentException> {
            checker.checkType(objectField.type, 10, context)
        }
        assertEquals("Expected O2 or ValueObjectBuilder<O2> for builder value for field objectField, got 10", e.message)
    }

    @Test
    fun testBackingData() {
        val backingDataField = o2Type.getField("backingDataField")
        val context = DynamicValueBuilderTypeChecker.FieldContext(backingDataField, o2Type)
        checker.checkType(backingDataField.type, "abc", context)

        var e = assertThrows<IllegalArgumentException> {
            checker.checkType(backingDataField.type, 123, context)
        }
        assertEquals("Expected value of type String for field backingDataField, got Int", e.message)

        val backingDataList = o1Type.getField("backingDataList")
        val context2 = DynamicValueBuilderTypeChecker.FieldContext(backingDataList, o1Type)
        checker.checkType(backingDataList.type, listOf(123), context2)

        e = assertThrows<IllegalArgumentException> {
            checker.checkType(backingDataList.type, listOf("abc"), context2)
        }
        assertEquals("Expected value of type Int for field backingDataList, got String", e.message)

        val invalidBackingData = o2Type.getField("invalidBackingData")
        val context3 = DynamicValueBuilderTypeChecker.FieldContext(invalidBackingData, o2Type)
        val ex = assertThrows<ViaductTenantUsageException> {
            checker.checkType(invalidBackingData.type, "abc", context3)
        }
        assertEquals("Backing data field invalidBackingData must have @backingData directive defined in schema. None found.", ex.message)
    }

    @Test
    fun testGlobalIDType() {
        val idField = userType.getField("id")
        val globalId = MockGlobalID(TestUser.Reflection, "456")
        val context = DynamicValueBuilderTypeChecker.FieldContext(idField, userType)
        checker.checkType(idField.type, globalId, context)

        val e = assertThrows<IllegalArgumentException> {
            checker.checkType(idField.type, 123, context)
        }
        assertEquals("Expected value of type GlobalID for field id, got Int", e.message)

        assertThrows<IllegalArgumentException> {
            checker.checkType(idField.type, null, context)
        }
    }

    @Test
    fun testScalarIDType() {
        val idField = testType.getField("id")
        val context = DynamicValueBuilderTypeChecker.FieldContext(idField, testType)

        checker.checkType(idField.type, "456", context)

        val e = assertThrows<IllegalArgumentException> {
            checker.checkType(idField.type, 123, context)
        }
        assertEquals("Expected value of type String for field id, got Int", e.message)
    }
}
