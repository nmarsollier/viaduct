package viaduct.utils.graphql

import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.language.SelectionSet
import graphql.parser.Parser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class GraphQLOssExtensionsTest {
    private val String.asDocument: Document get() = Parser.parse(this)

    @Test
    fun `Node_allChildren`() {
        val children = "{x}".asDocument.allChildren
        assertEquals(3, children.size)
        assertInstanceOf(OperationDefinition::class.java, children[0])
        assertInstanceOf(SelectionSet::class.java, children[1])
        assertInstanceOf(Field::class.java, children[2])
    }

    @Test
    fun `Node_allChildrenOfType`() {
        val doc = "{ x { y } }".asDocument
        assertEquals(1, doc.allChildrenOfType<OperationDefinition>().size)
        assertEquals(2, doc.allChildrenOfType<Field>().size)
        assertEquals(2, doc.allChildrenOfType<SelectionSet>().size)
    }
}
