package viaduct.testapps.resolver

import org.junit.jupiter.api.Test
import viaduct.graphql.test.assertJson
import viaduct.testapps.fixtures.ScopedSchemaInfo
import viaduct.testapps.fixtures.TestTenantPackageFinder
import viaduct.testapps.testfixtures.TestBase

class ScalarFailureResolverTests : TestBase(
    setOf(ScopedSchemaInfo(DEFAULT_SCHEMA_ID, setOf(DEFAULT_PUBLIC_SCOPE_ID))),
    tenantPackageFinder = TestTenantPackageFinder(Tenants),
) {
    @Test
    fun scalarFailureEnumResolverTestNullPointer() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { scalarFailureEnum }"
        ).assertJson(
            """{"errors":[{"message":"java.lang.NullPointerException: null","locations":[{"line":1,"column":19}],"path":["scalarFailureEnum"],"extensions":{"fieldName":"scalarFailureEnum","parentType":"Query","operationName":"TestQuery","classification":"DataFetchingException"}}],"data":null}"""
        )
    }

    @Test
    fun scalarFailureStringResolverThrownException() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { scalarFailureString }"
        ).assertJson(
            """{"errors":[{"message":"java.lang.RuntimeException: Error Occurred","locations":[{"line":1,"column":19}],"path":["scalarFailureString"],"extensions":{"fieldName":"scalarFailureString","parentType":"Query","operationName":"TestQuery","classification":"DataFetchingException"}}],"data":null}"""
        )
    }

    @Test
    fun scalarFailureIntResolverStackOverflow() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { scalarFailureInt }"
        ).assertJson(
            """{"errors":[{"message":"java.lang.StackOverflowError: null","locations":[{"line":1,"column":19}],"path":["scalarFailureInt"],"extensions":{"fieldName":"scalarFailureInt","parentType":"Query","operationName":"TestQuery","classification":"DataFetchingException"}}],"data":null}"""
        )
    }

    @Test
    fun scalarFailureNoResolverForField() {
        execute(
            schemaId = DEFAULT_SCHEMA_ID,
            query = "query TestQuery { scalarFailureNoResolver }"
        ).assertJson(
            """{"errors":[{"message":"Validation error (FieldUndefined@[scalarFailureNoResolver]) : Field 'scalarFailureNoResolver' in type 'Query' is undefined","locations":[{"line":1,"column":19}],"extensions":{"classification":"ValidationError"}}],"data":null}"""
        )
    }
}
