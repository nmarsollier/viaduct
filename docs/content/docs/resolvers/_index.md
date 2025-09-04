---
title: Resolvers
description: Understanding resolvers in Viaduct
weight: 10
---

In Viaduct, all module code is provided in the form of either a *node resolver* or a *field resolver*. Node and field resolvers are implemented similarly:

* **Schema**: The schema is the source of truth for what resolvers should exist. Define node types and add the `@resolver` directive to fields you want to provide resolvers for.
* **Generated base classes**: Viaduct generates abstract classes for all node and field resolvers based on the schema.
* **Implementation**: Implement a resolver by providing a subclass of the generated base class, and overriding either `resolve` or `batchResolve`.

## Responsibility sets

Responsibility sets (also known as "responsibility selection sets") are an important concept in the Viaduct API. Every node and field resolver is responsible for resolving the fields in its responsibility set. This is all fields, including nested fields, that themselves do not have a resolver. The node and field resolver pages provide more details with examples.

## Injection

Viaduct is designed to create instances of resolver classes through dependency injection. You can define this behavior by implementing this interface:

```kotlin
interface TenantCodeInjector {
    fun <T> getProvider(clazz: Class<T>): Provider<T>
}
```

By default (primarily for tests), Viaduct will assume that a zero-argument constructor will be available for any resolver you may need to write, and Viaduct will automatically construct a single instance of this class to be used whenever the resolver is called.  Note that this is not guaranteed to be thread-safe, so it is not recommended as your main approach.

Examples of using this dependency injection mechanism are available in the demo applications.
