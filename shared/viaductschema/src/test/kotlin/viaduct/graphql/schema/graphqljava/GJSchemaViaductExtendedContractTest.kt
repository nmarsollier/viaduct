package viaduct.graphql.schema.graphqljava

import viaduct.graphql.schema.test.ViaductExtendedSchemaContract

class GJSchemaViaductExtendedContractTest : ViaductExtendedSchemaContract {
    override fun makeSchema(schema: String): GJSchema = GJSchema.fromRegistry(readTypes(schema))
}
