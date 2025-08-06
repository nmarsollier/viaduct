package viaduct.graphql.schema.graphqljava

import viaduct.graphql.schema.ViaductExtendedSchema
import viaduct.graphql.schema.test.ViaductExtendedSchemaContract

class GJSchemaRawViaductExtendedContractTest : ViaductExtendedSchemaContract {
    override fun makeSchema(schema: String): ViaductExtendedSchema = GJSchemaRaw.fromRegistry(readTypes(schema))
}
