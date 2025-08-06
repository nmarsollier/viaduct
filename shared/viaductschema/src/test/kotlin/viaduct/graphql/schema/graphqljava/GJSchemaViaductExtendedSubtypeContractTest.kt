package viaduct.graphql.schema.graphqljava

import viaduct.graphql.schema.test.ViaductExtendedSchemaSubtypeContract

class GJSchemaViaductExtendedSubtypeContractTest : ViaductExtendedSchemaSubtypeContract() {
    override fun getSchemaClass() = GJSchema::class
}
