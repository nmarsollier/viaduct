---
title: Mutations
description: Mutating data in Viaduct.
weight: 40
---

`@resolver` is used for fields on `Mutation` as well as for `Query`.  For example:

```graphql
extend type Mutation {
    myMutation(userId: ID! @idOf(type: "User")): User @resolver
}
```

The resolver for this mutation might look like:

```kotlin
@Resolver
class MyMutationResolver @Inject constructor(
    val myClient: MyClient
) : MutationResolvers.MyMutation() {
    fun resolve(ctx: Context): User {
        myClient.mutate(ctx.arguments.userId.internalID)
        return ctx.nodeFor(ctx.arguments.userId) // Note use of node reference
    }
}
```

As this example shows, resolvers for mutations are almost identical to field resolvers.  A major difference is that `Context` implements  [`MutationFieldExecutionContext`](https://github.com/airbnb/viaduct/blob/main/tenant/api/src/main/kotlin/viaduct/api/context/MutationFieldExecutionContext.kt). This interface provides one additional function, `MutationFieldExecutionContext.mutation()`, which can be used to execute a submutation within your mutation, similar to using `FieldResolverContext.query()` to execute a subquery (which you can also do in a mutation field resolver).

Mutation resolvers should still be annotated with `@Resolver` as shown above. However, a required selection set may *not* be given in this  `@Resolver` annotation, as those would be selections on other mutation fields.  Again, a mutation resolver can call other mutations by calling `ctx.mutation`.
