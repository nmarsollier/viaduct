package viaduct.graphql.schema.graphqljava

import viaduct.graphql.schema.FilteredSchema
import viaduct.graphql.schema.SchemaInvariantOptions

class FilteredSchemaRootTypeTests : RootTypeFactoryContractForBoth {
    override fun makeSchema(
        schema: String,
        queryTypeName: String?,
        mutationTypeName: String?,
        subscriptionTypeName: String?
    ) = with(GJSchema.fromRegistry(readTypes(schema))) {
        if (queryTypeName == null && mutationTypeName == null && subscriptionTypeName == null) {
            this.filter(NoopSchemaFilter())
        } else {
            FilteredSchema(
                NoopSchemaFilter(),
                this.types.entries,
                this.directives.entries,
                SchemaInvariantOptions.DEFAULT,
                queryTypeName,
                mutationTypeName,
                subscriptionTypeName
            )
        }
    }
}
