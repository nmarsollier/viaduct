package viaduct.tenant.runtime.featuretests.fixtures

class FeatureTestSchemaFeatureAppTest {
    val sdl: String = """
#START_SCHEMA
# This schema defines fixture types that are used in end-to-end tests

directive @resolver on FIELD_DEFINITION

interface Node {
  id: ID!
}

type Query {
  string1: String @resolver
  string2: String @resolver
  hasArgs1(x: Int!): Int! @resolver
  hasArgs2(x: String!): String! @resolver
  foo: Foo @resolver
  bar: Bar @resolver
  baz: Baz @resolver
  bazList: [Baz] @resolver
  iface: Interface @resolver
  union_: Union @resolver
  boo: Boo @resolver
  node: Node @resolver
}

type Mutation {
  string1: String @resolver
  string2: String @resolver
}

interface Interface {
  value: String
}

union Union = Foo | Bar | Baz

type Foo implements Interface {
  value: String @resolver
  bar: Bar @resolver
}
type Bar implements Interface {
  value: String @resolver
  foo: Foo @resolver
}

type Baz implements Node {
  id: ID!
  x: Int
  x2: String
  y: String @resolver
  anotherBaz: Baz @resolver
  z: Int @resolver
}

type Boo {
  value: Int
}

#END_SCHEMA
    """.trimIndent()
}
