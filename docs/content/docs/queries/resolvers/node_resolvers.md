---
title: Node Resolvers
description: Writing resolvers for nodes in Viaduct.
weight: 2
---

When implementing a node resolver you have access to a `NodeExecutionContext`:

```kotlin
interface NodeExecutionContext<T: Node>: ExecutionContext {
  val id: GlobalID<T>
  fun selections(): SelectionSet<T>
}
```

In addition to the functions provided by `ExecutionContext`, `NodeExecutionContext` gives access to the identifier of the node to be resolved, and to the selection-set being requested by the query.  Most node resolvers are not "selective," i.e., they ignore this selection set and thus don’t call this function.  In this case, as discussed above, it’s important that the node resolver returns its entire responsibility set.

**Advanced Users:** If the `selections` function is *not* called by an invocation of a resolver, then the engine will assume that invocation will return the full responsibility set of the resolver and may take actions based on that assumption.  If a resolver is going to be selective, then it **must** call this function to get its selection set rather than obtain it through some other means.

You supply the implementation of a node resolver by subclassing its resolver base class. The base class has two functions you can choose to override, `resolve` or `batchResolve`. You must override one and only one of the two functions, not both:

```kotlin
@Resolver
class UserNodeResolver @Inject constructor(
  val userService: UserServiceClient
): NodeResolvers.User() {
  override suspend fun resolve(ctx: Context): User {
    val data = userService.fetch(ctx.id.internalId)
    return User.builder(ctx)
      .id(ctx.id)
      .firstName(data.firstName)
      .lastName(data.lastName)
      .build()
  }
}
```

Or,

```kotlin
@Resolver
class UserNodeResolver @Inject constructor(
  val userService: UserServiceClient
): NodeResolvers.User() {
  override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<User>> {
    val ids = contexts.map { it.id.internalId }
    val responses = userService.fetch(ids)
    return users.map { response ->
      FieldValue.ofValue(
        User.builder(ctx)
          .id(response.id)
          .firstName(response.firstName)
          .lastName(response.lastName)
          .build()
      )
    }
  }
}
```

Points illustrated by this example:

* The `@Resolver` annotation is always required on both node and field resolver classes.
* Dependency injection can be used to provide access to values beyond what’s in the execution context.
* As mentioned previously, when building the GRT for a GraphQL object type, you do *not* have to provide values for all fields, and indeed should not provide values for fields outside of the resolvers responsibility set (the `displayName` field in our example).
