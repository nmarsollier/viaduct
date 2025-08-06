package viaduct.graphql.schema

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppliedDirectiveTest {
    @Test
    fun `AppliedDirective smoke test`() {
        val args = mapOf("foo" to "bar", "baz" to 1)
        val appliedDirective = ViaductSchema.AppliedDirective.of("name", args)
        assertEquals(appliedDirective.name, "name")
        assertEquals(appliedDirective.arguments.entries, args.entries)
    }
}
