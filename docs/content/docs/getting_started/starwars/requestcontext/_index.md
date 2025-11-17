---
title: Request Context
description: Managing request-scoped data and context in Viaduct applications.
layout: docs/single
weight: 6
---

Viaduct provides comprehensive support for managing request-specific context throughout your GraphQL operations. This is essential for handling authentication, authorization, tenant isolation, and other request-scoped concerns.

## Request context approaches

Viaduct supports multiple approaches to managing request context, allowing you to choose the pattern that best fits your application architecture.

### 1. Framework-based request scoping

The recommended approach is to leverage your web framework's built-in request scoping mechanisms. This provides automatic lifecycle management and prevents context leakage between requests.

#### Example with Micronaut

Create a request-scoped service:

{{< codetag path="demoapps/starwars/common/src/main/kotlin/com/example/starwars/common/SecurityAccessContext.kt" tag="request_context">}}

Populate the context in your GraphQL controller:

{{< codetag path="demoapps/starwars/src/main/kotlin/com/example/starwars/service/viaduct/ViaductRestController.kt" tag="viaduct_graphql_controller">}}

Use the context in your resolvers via dependency injection:

{{< codetag path="demoapps/starwars/modules/filmography/src/main/kotlin/com/example/starwars/modules/filmography/characters/mutations/CreateCharacterMutation.kt" tag="create_example">}}

**Benefits:**
- Automatic lifecycle management
- Type-safe access to context data
- Prevents data leakage between requests
- Leverages framework's dependency injection
- Clean separation of concerns

### 2. ExecutionInput request context

For simpler installations or when you need to pass arbitrary context without setting up framework scoping, you can use `ExecutionInput.requestContext`.

#### Setting the request context

The `ExecutionInput` interface includes a `requestContext` property:


When creating your `ExecutionInput`, include the request context:

{{< codetag path="demoapps/starwars/src/main/kotlin/com/example/starwars/service/viaduct/ViaductRestController.kt" tag="create_execution_input">}}


```kotlin
 val requestContext = mapOf(
  "securityAccess" to securityAccess,
  "tenantId" to tenantId
 )
 val executionInput = ExecutionInput.create(
  operationText = request["query"] as String,
  variables = (request["variables"] as? Map<String, Any>) ?: emptyMap(),
  requestContext = requestContext
 )
```

#### Accessing request context in resolvers

Inside your resolvers, access the request context through the resolver context:

```kotlin
@Resolver
class FooQuery : FooResolber.Character() {
    override suspend fun resolve(ctx: Context): Foo? {
        val requestContext = ctx.requestContext as? Map<*, *>
        ...
    }
}
```

**Benefits:**
- Simple to set up
- No additional framework configuration needed
- Direct access to context data
- Flexible for passing arbitrary data

**Considerations:**
- Requires manual type casting
- Less type-safe than framework approach
- No automatic lifecycle management
- Context must be manually threaded through execution

## Choosing an approach

For production applications with complex authorization needs, authentication, or multi-tenancy, use framework-based request scoping. It provides automatic lifecycle management and type safety.

For simpler installations or prototypes where you need to pass arbitrary context without framework configuration, use `ExecutionInput.requestContext`.
{{< prevnext >}}
