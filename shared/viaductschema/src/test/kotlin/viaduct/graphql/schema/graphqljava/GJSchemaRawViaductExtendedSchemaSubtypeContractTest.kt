package viaduct.graphql.schema.graphqljava

import viaduct.graphql.schema.test.ViaductExtendedSchemaSubtypeContract

class GJSchemaRawViaductExtendedSchemaSubtypeContractTest : ViaductExtendedSchemaSubtypeContract() {
    override fun getSchemaClass() = GJSchemaRaw::class
}
