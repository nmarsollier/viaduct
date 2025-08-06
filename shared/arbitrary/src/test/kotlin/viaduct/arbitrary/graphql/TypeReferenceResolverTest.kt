package viaduct.arbitrary.graphql

import graphql.Scalars
import graphql.schema.GraphQLTypeReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TypeReferenceResolverTest {
    private val ref = GraphQLTypeReference("Int")

    @Test
    fun empty() {
        val resolver = TypeReferenceResolver.fromTypes(GraphQLTypes.empty)
        assertNull(resolver(ref))
        assertNull(resolver.resolve(ref))
        assertThrows<IllegalArgumentException> {
            resolver.resolveOrThrow(ref)
        }
    }

    @Test
    fun fromTypes() {
        val type = Scalars.GraphQLInt
        val resolver = TypeReferenceResolver.fromTypes(GraphQLTypes.empty.copy(scalars = mapOf("Int" to type)))
        assertEquals(type, resolver(ref))
        assertEquals(type, resolver.resolve(ref))
        assertEquals(type, resolver.resolveOrThrow(ref))
    }

    @Test
    fun fromSchema() {
        val resolver = TypeReferenceResolver.fromSchema(mkGJSchema(""))
        val type = Scalars.GraphQLInt
        assertEquals(type, resolver(ref))
        assertEquals(type, resolver.resolve(ref))
        assertEquals(type, resolver.resolveOrThrow(ref))
    }

    @Test
    fun none() {
        val resolver = TypeReferenceResolver.none
        assertThrows<UnsupportedOperationException> {
            resolver(ref)
        }
    }
}
