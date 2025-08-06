package viaduct.engine.runtime.execution

import graphql.schema.GraphQLFieldDefinition
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TenantNameResolverTest {
    private lateinit var tenantNameResolver: TenantNameResolver
    private lateinit var fieldDefinition: GraphQLFieldDefinition

    @BeforeEach
    fun setUp() {
        tenantNameResolver = TenantNameResolver()
        fieldDefinition = mockk()
    }

    @Test
    fun `test resolve method returns null`() {
        every { fieldDefinition.getAppliedDirective(any())?.getArgument("name")?.getValue() as String? } returns null

        val result = tenantNameResolver.resolve(fieldDefinition)
        assertNull(result)
    }
}
