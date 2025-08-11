package viaduct.engine.runtime.tenantloading

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import viaduct.engine.api.Coordinate
import viaduct.engine.api.RequiredSelectionSet
import viaduct.engine.api.mocks.MockCheckerExecutor
import viaduct.engine.api.mocks.MockSchema
import viaduct.engine.api.mocks.mkRSS

class CheckerSelectionSetsAreProperlyTypedTest {
    @Test
    fun `valid -- no selection sets`() {
        assertValid("User", "name", emptyMap())
    }

    @Test
    fun `valid -- single selection set with correct type`() {
        val rss = mkRSS("User", "id name")
        assertValid("User", "name", mapOf("key1" to rss))
    }

    @Test
    fun `valid -- multiple selection sets with correct type`() {
        val rss1 = mkRSS("User", "id")
        val rss2 = mkRSS("User", "name email")
        assertValid("User", "name", mapOf("key1" to rss1, "key2" to rss2))
    }

    @Test
    fun `valid -- selection sets with null values`() {
        val rss = mkRSS("User", "id")
        assertValid("User", "name", mapOf("key1" to rss, "key2" to null))
    }

    @Test
    fun `invalid -- single selection set with wrong type`() {
        val rss = mkRSS("Query", "currentUser") // Wrong! Should be "User"
        assertInvalid("User", "name", mapOf("key1" to rss), "Query")
    }

    @Test
    fun `invalid -- multiple selection sets with wrong types`() {
        val rss1 = mkRSS("Query", "currentUser") // Wrong! Should be "User"
        val rss2 = mkRSS("Post", "title") // Wrong! Should be "User"
        assertInvalid("User", "name", mapOf("key1" to rss1, "key2" to rss2), "Query", "Post")
    }

    @Test
    fun `invalid -- mixed valid and invalid selection sets`() {
        val validRss = mkRSS("User", "id") // Correct
        val invalidRss = mkRSS("Query", "currentUser") // Wrong! Should be "User"
        assertInvalid("User", "name", mapOf("valid" to validRss, "invalid" to invalidRss), "Query")
    }

    @Test
    fun `invalid -- multiple different wrong types`() {
        val rss1 = mkRSS("Query", "currentUser")
        val rss2 = mkRSS("Post", "title")
        val rss3 = mkRSS("Comment", "body")
        assertInvalid(
            "User",
            "name",
            mapOf("key1" to rss1, "key2" to rss2, "key3" to rss3),
            "Query",
            "Post",
            "Comment"
        )
    }

    /**
     * Helper method to validate checker selection sets and assert no exception is thrown
     */
    private fun assertValid(
        typeName: String,
        fieldName: String,
        requiredSelectionSets: Map<String, RequiredSelectionSet?>
    ) {
        assertDoesNotThrow {
            validateCheckerSelectionSets(typeName, fieldName, requiredSelectionSets)
        }
    }

    /**
     * Helper method to validate checker selection sets and assert an exception is thrown
     * containing the specified invalid type names
     */
    private fun assertInvalid(
        typeName: String,
        fieldName: String,
        requiredSelectionSets: Map<String, RequiredSelectionSet?>,
        vararg expectedInvalidTypeNames: String
    ) {
        val exception = assertThrows<Exception> {
            validateCheckerSelectionSets(typeName, fieldName, requiredSelectionSets)
        }

        for (invalidTypeName in expectedInvalidTypeNames) {
            assertTrue(
                exception.message!!.contains(invalidTypeName),
                "Exception message should contain invalid type name '$invalidTypeName'. " +
                    "Actual message: ${exception.message}"
            )
        }
    }

    /**
     * Creates a CheckerExecutorValidationCtx and validates it using CheckerSelectionSetsAreProperlyTyped
     */
    private fun validateCheckerSelectionSets(
        typeName: String,
        fieldName: String,
        requiredSelectionSets: Map<String, RequiredSelectionSet?>
    ) {
        val schema = MockSchema.mk(
            """
            type Query {
                empty: Int
                currentUser: User
                globalFlag: String
            }
            type User {
                id: ID!
                name: String
                email: String
            }
            type Post {
                title: String
                author: User
            }
            type Comment {
                body: String
                post: Post
            }
            """.trimIndent()
        )

        val checker = MockCheckerExecutor(requiredSelectionSets = requiredSelectionSets)

        val ctx = CheckerExecutorValidationCtx(
            coord = Coordinate(typeName, fieldName),
            requiredSelectionSets = checker.requiredSelectionSets
        )

        val validator = CheckerSelectionSetsAreProperlyTyped(schema)
        validator.validate(ctx)
    }
}
