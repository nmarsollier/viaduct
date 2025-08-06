package viaduct.graphql.schema.graphqljava

import viaduct.graphql.schema.ViaductExtendedSchema
import viaduct.graphql.schema.test.ViaductExtendedSchemaContract

class FilteredSchemaViaductExtendedContractTest : ViaductExtendedSchemaContract {
    override fun makeSchema(schema: String): ViaductExtendedSchema = GJSchema.fromRegistry(readTypes(schema)).filter(NoopSchemaFilter())
}
