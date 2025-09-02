@file:Suppress("unused", "ClassName")

package viaduct.tenant.runtime.execution.idof

import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.api.globalid.GlobalID
import viaduct.graphql.test.assertEquals
import viaduct.tenant.runtime.execution.idof.resolverbases.QueryResolvers
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase

@Resolver
class Query_UserResolver : QueryResolvers.User() {
    override suspend fun resolve(ctx: Context): User {
        return User.Builder(ctx)
            .id(ctx.arguments.id as GlobalID<User>)
            .name("Alice")
            .build()
    }
}

class IdOfFeatureAppTest : FeatureAppTestBase() {
    override var sdl = """
        |#START_SCHEMA
        | directive @resolver on FIELD_DEFINITION
        | directive @idOf(type: String!) on FIELD_DEFINITION | INPUT_FIELD_DEFINITION | ARGUMENT_DEFINITION
        | type Query {
        |   user(id: ID! @idOf(type: "User")): User @resolver
        | }
        |
        | interface Node {
        |   id: ID!
        | }
        |
        | type User implements Node {
        |   id: ID!
        |   name: String
        | }
        |#END_SCHEMA
    """.trimMargin()

    @Test
    fun `idOf directive works when a valid user id is used`() {
        val generatedId = createGlobalIdString(User.Reflection, "123")

        execute(
            query = """
                query {
                    user(id: "$generatedId") {
                        id
                        name
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "user" to {
                    "id" to generatedId
                    "name" to "Alice"
                }
            }
        }
    }

    @Test
    fun `idOf directive throws the correct error message when an invalid global ID is used`() {
        execute(
            query = """
                query {
                    user(id: "123") {
                        id
                        name
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "user" to null
            }
            "errors" to arrayOf(
                {
                    "message" to "viaduct.api.ViaductFrameworkException: InputLikeBase.get failed for Query_User_Arguments.id " +
                        "(java.lang.IllegalArgumentException: Expected GlobalID \"123\" to be a Base64-encoded string with the decoded format " +
                        "'<type name>:<internal ID>', got decoded value �m)"
                    "locations" to arrayOf(
                        {
                            "line" to 2
                            "column" to 5
                        }
                    )
                    "path" to listOf("user")
                    "extensions" to {
                        "fieldName" to "user"
                        "parentType" to "Query"
                        "classification" to "DataFetchingException"
                    }
                }
            )
        }
    }
}
