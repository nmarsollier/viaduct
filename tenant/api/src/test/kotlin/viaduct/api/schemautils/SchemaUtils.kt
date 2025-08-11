@file:Suppress("UnstableApiUsage")

package viaduct.api.schemautils

import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import viaduct.api.testschema.ApiTestSchemaFeatureAppTest
import viaduct.engine.api.ViaductSchema

object SchemaUtils {
    fun getSchema(): ViaductSchema {
        val schemaContent = ApiTestSchemaFeatureAppTest().sdl
            .substringAfter("#START_SCHEMA")
            .substringBefore("#END_SCHEMA")
            .lines()
            .map { it.removePrefix("    | ") }
            .joinToString("\n")
            .trim()
        return mkSchema(schemaContent)
    }

    private fun mkSchema(sdl: String): ViaductSchema {
        val tdr = SchemaParser().parse(sdl)
        return ViaductSchema(SchemaGenerator().makeExecutableSchema(tdr, RuntimeWiring.MOCKED_WIRING))
    }
}
