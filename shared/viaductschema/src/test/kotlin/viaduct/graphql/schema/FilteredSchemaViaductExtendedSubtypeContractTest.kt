package viaduct.graphql.schema

import viaduct.graphql.schema.test.ViaductExtendedSchemaSubtypeContract

class FilteredSchemaViaductExtendedSubtypeContractTest : ViaductExtendedSchemaSubtypeContract() {
    override fun getSchemaClass() = FilteredSchema::class

    override val skipExtensionTests = true
}
