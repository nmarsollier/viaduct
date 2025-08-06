package viaduct.tenant.runtime.context.factory

import viaduct.tenant.runtime.fixtures.FeatureAppTestBase

class ContextFactoryFeatureAppTest : FeatureAppTestBase() {
    override var sdl =
        """
        #START_SCHEMA
        type Query {
            empty: Int
        }

        interface Node {
          id: ID!
        }

        type Foo {
          fieldWithArgs(x: Int, y: Boolean!, z: String = ""): Int
        }

        type Bar {
          x: Int
          y: Boolean!
          z: String
          bar: Bar
          bars: [Bar]
        }

        type Baz implements Node {
          id: ID!
          x: Int
        }

        type Mutation {
          mutate(x: Int!): Int!
        }

        input Input {
          x: Int
          y: Boolean!
          z: String = ""
        }
        #END_SCHEMA
        """
}
