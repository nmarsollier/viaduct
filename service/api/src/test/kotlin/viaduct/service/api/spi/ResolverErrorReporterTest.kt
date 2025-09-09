package viaduct.service.api.spi

import org.junit.jupiter.api.Assertions
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

        Assertions.assertEquals("testField", map["fieldName"])
        Assertions.assertEquals("TestType", map["parentType"])
        Assertions.assertEquals("TestOperation", map["operationName"])
        Assertions.assertEquals("true", map["isFrameworkError"])
        Assertions.assertEquals(listOf("Resolver1", "Resolver2").joinToString(" > "), map["resolvers"])
        Assertions.assertEquals("SomeErrorType", map["errorType"])
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
        Assertions.assertTrue(str.contains("testField"))
        Assertions.assertTrue(str.contains("TestType"))
        Assertions.assertTrue(str.contains("TestOperation"))
        Assertions.assertTrue(str.contains("true"))
        Assertions.assertTrue(str.contains("Resolver1"))
        Assertions.assertTrue(str.contains("Resolver2"))
        Assertions.assertTrue(str.contains("SomeErrorType"))
    }
}
