package viaduct.graphql.schema.graphqljava

class GRSchemaRawRootTypeTests : RootTypeFactoryContractForRaw {
    override fun makeSchema(
        schema: String,
        queryTypeName: String?,
        mutationTypeName: String?,
        subscriptionTypeName: String?
    ) = GJSchemaRaw.fromRegistry(
        readTypes(schema),
        queryTypeName = queryTypeName,
        mutationTypeName = mutationTypeName,
        subscriptionTypeName = subscriptionTypeName
    )
}
