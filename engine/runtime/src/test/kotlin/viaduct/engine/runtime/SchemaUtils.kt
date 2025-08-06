package viaduct.engine.runtime

import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.intellij.lang.annotations.Language

fun mkSchema(
    @Language("GraphQL") sdl: String
): GraphQLSchema {
    val tdr = SchemaParser().parse(sdl)
    return SchemaGenerator().makeExecutableSchema(tdr, RuntimeWiring.MOCKED_WIRING)
}
