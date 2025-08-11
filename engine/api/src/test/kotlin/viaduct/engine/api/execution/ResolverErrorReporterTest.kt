package viaduct.engine.api.execution

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResolverErrorReporterTest {
    @Test
    fun testToMap() {
        val metadata = ResolverErrorReporter.Companion.ErrorMetadata(
            fieldName = "testField",
            parentType = "TestType",
            operationName = "TestOperation",
            isFrameworkError = true,
            resolvers = listOf("Resolver1", "Resolver2"),
            errorType = "SomeErrorType"
        )
        val map = metadata.toMap()

        assertEquals("testField", map["fieldName"])
        assertEquals("TestType", map["parentType"])
        assertEquals("TestOperation", map["operationName"])
        assertEquals("true", map["isFrameworkError"])
        assertEquals(listOf("Resolver1", "Resolver2").joinToString(" > "), map["resolvers"])
        assertEquals("SomeErrorType", map["errorType"])
    }

    @Test
    fun testToString() {
        val metadata = ResolverErrorReporter.Companion.ErrorMetadata(
            fieldName = "testField",
            parentType = "TestType",
            operationName = "TestOperation",
            isFrameworkError = true,
            resolvers = listOf("Resolver1", "Resolver2"),
            errorType = "SomeErrorType"
        )
        val str = metadata.toString()
        assertTrue(str.contains("testField"))
        assertTrue(str.contains("TestType"))
        assertTrue(str.contains("TestOperation"))
        assertTrue(str.contains("true"))
        assertTrue(str.contains("Resolver1"))
        assertTrue(str.contains("Resolver2"))
        assertTrue(str.contains("SomeErrorType"))
    }
}
