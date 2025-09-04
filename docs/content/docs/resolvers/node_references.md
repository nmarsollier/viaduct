---
title: Node References
description: Creating node references in resolvers
weight: 5
---

GraphQL resolvers often need to link to another `Node`. For example, the `Listing.host` field might have node type `User`.  In this case, the service call that backs the referring node (the service for `Listing`s in our example) will typically have an *id* for the other node and needs to inform Viaduct that this id should be used to resolve the other node (the `User` in our example).

This is achieved by calling `Context.nodeFor` to obtain a *node reference.*  The `nodeFor` function takes a global id for a node and returns (in our example) a special GRT for that node.  In our example, if this is the schema for `Listing`:

```graphql
type Listing implements Node {
    id: ID!
    hostId: ID! @idOf(type: "User")
    host: User
    ...other fields
}
```

In the node resolver for `Listing` we would have:

```kotlin
@Resolver
class ListingNodeResolver @Inject constructor(val client: ListingClient) : Nodes.Listing() {
    override suspend fun resolve(ctx: Context): TaxProfile {
        val data = client.fetch(ctx.id.internalID)
        return ctx.builderFor(Listing::class)
            .id(ctx.id)
            .host(ctx.nodeFor(ctx.globalIDFor(User::class, data.hostID)))
            /* ... other fields populated from [data] */
            .build()
    }
}
```

When this resolver returns, the Viaduct engine will invoke the `User` node resolver to fetch any data needed for the `Listing.host` field.

This example illustrates a subtle aspect of the "responsibility set" of resolvers, which is that the "responsibility" for resolving a field that has node type is split between the resolver whose responsibility set contains the field, and the resolver of the node being returned.  Specifically, the containing resolver is responsible for returning a node reference as illustrated here, effectively resolving the node’s `id` field.  From there, the node-resolver takes over to resolve the rest of the node’s responsibility set.

We noted above that the GRT returned by `nodeFor` is "special."  It’s special because, in the code that calls `nodeFor`, *only* the `id` field is set; all other fields are not set and will throw an exception on an attempt to read them.  If for some reason a resolver needs a *resolved* node rather than a node reference, the resolver can use a subquery, which is described in the next subsection.
