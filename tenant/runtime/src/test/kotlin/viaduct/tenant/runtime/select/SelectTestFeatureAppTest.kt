package viaduct.tenant.runtime.select

import graphql.schema.idl.SchemaParser
import graphql.schema.idl.UnExecutableSchemaGenerator
import viaduct.engine.api.ViaductSchema
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase

class SelectTestFeatureAppTest : FeatureAppTestBase() {
    companion object {
        val schema: ViaductSchema by lazy {
            ViaductSchema(
                UnExecutableSchemaGenerator.makeUnExecutableSchema(
                    SchemaParser().parse(SelectTestFeatureAppTest().sdl)
                )
            )
        }
    }

    override var sdl =
        """
        #START_SCHEMA
        type Query {
          node: Node
          intField: Int
        }
        type Mutation {
          mutfield(x: Int!): String!
        }

        interface Node {
          id: ID!
          nodeSelf: Node!
        }

        type Foo implements Node {
          id: ID!
          nodeSelf: Node!
          fooId: ID!
          fooSelf: Foo!
        }

        type Bar {
          bar: Bar!
        }

        union FooOrBar = Foo | Bar
        #END_SCHEMA
        """
}
