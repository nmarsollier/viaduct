package viaduct.testapps.policycheck

import java.util.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertEquals
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

class FieldPolicyCheckTests : TestBase(
    setOf(ScopedSchemaInfo("policyCheckSchema", setOf("SCOPE1"))),
    fullSchemaRegex = "viaduct.testapps.policycheck.tenant1.*graphqls",
    tenantPackageFinder = TestTenantPackageFinder(Tenants),
) {
    @Test
    fun `type returns if policy check passes on referenced node`() {
        val internalId = "person1"
        val globalId = Base64.getEncoder().encodeToString("CanAccessPerson:$internalId".toByteArray())

        execute(
            schemaId = "policyCheckSchema",
            query = "query { canAccessTypeByReference(id: \"${internalId}\") { id name ssn } }"
        ).assertEquals {
            "data" to {
                "canAccessTypeByReference" to {
                    "id" to globalId
                    "name" to "john"
                    "ssn" to "social security number"
                }
            }
        }
    }

    @Test
    fun `throws if type is not accessible on referenced node`() {
        val result = execute(
            schemaId = "policyCheckSchema",
            query = "query { canNotAccessTypeByReference(id: \"person1\") { id name ssn } }"
        )
        assertNull(result.getData())
        assertEquals(1, result.errors.size)
        result.errors.first().let { err ->
            assertEquals(listOf("canNotAccessTypeByReference"), err.path)
            assertTrue(err.message.contains("This field is not accessible"))
        }
    }

    @Test
    fun `type should return null if policy check fails`() {
        val result = execute(
            schemaId = "policyCheckSchema",
            query = "query { canNotAccessType { name ssn } }"
        )
        assertNull(result.getData())
        assertEquals(1, result.errors.size)
        result.errors.first().let { err ->
            assertEquals(listOf("canNotAccessType"), err.path)
            assertTrue(err.message.contains("This field is not accessible"))
        }
    }

    @Test
    fun `field returns if policy check passes`() {
        execute(
            schemaId = "policyCheckSchema",
            query = "query { canAccessField }"
        ).assertEquals {
            "data" to { "canAccessField" to "can see field" }
        }
    }

    @Test
    fun `field does not return if policy check fails`() {
        val result = execute(
            schemaId = "policyCheckSchema",
            query = "query { canNotAccessField }"
        )
        assertEquals(mapOf("canNotAccessField" to null), result.toSpecification()["data"])
        assertEquals(1, result.errors.size)
        val error = result.errors[0]
        assertTrue(error.message.contains("Exception while fetching data (/canNotAccessField) : This field is not accessible"))
        assertEquals(listOf("canNotAccessField"), error.path)
    }
}
