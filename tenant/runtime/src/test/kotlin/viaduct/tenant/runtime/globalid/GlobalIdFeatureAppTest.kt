package viaduct.tenant.runtime.globalid

import graphql.schema.idl.SchemaParser
import graphql.schema.idl.UnExecutableSchemaGenerator
import viaduct.engine.api.ViaductSchema
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase

class GlobalIdFeatureAppTest : FeatureAppTestBase() {
    companion object {
        val schema: ViaductSchema by lazy {
            ViaductSchema(
                UnExecutableSchemaGenerator.makeUnExecutableSchema(
                    SchemaParser().parse(GlobalIdFeatureAppTest().sdl)
                )
            )
        }
    }

    override var sdl =
        """
        #START_SCHEMA
        type User implements Node {
          id: ID!
          name: String!
          email: String!
        }

        interface Node {
          id: ID!
        }

        input CreateUserInput {
          id: ID!
          name: String!
          email: String!
        }

        type Query {
          user(id: ID!): User
        }

        type Mutation {
          createUser(input: CreateUserInput!): User
        }

        type Foo {
          id: ID!
          name: String!
        }
        #END_SCHEMA
        """
}
