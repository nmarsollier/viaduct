package viaduct.api.types

import graphql.schema.GraphQLEnumType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import viaduct.api.schemautils.SchemaUtils

class InputTest {
    @Test
    fun testInputType() {
        val inputType = Input.inputType("Input1", SchemaUtils.getSchema())
        assertTrue(inputType.getField("enumFieldWithDefault").type is GraphQLEnumType)
    }
}
