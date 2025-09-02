---
title: Resolvers
description: Understanding resolvers in Viaduct.
weight: 2
---

In Viaduct, *all* tenant code is provided in the form of either a *node resolver* or a *field resolver*.  You implement either kind of resolver by providing a subclass of a *resolver base class* that we generate for you.  Resolver base classes are best understood through the examples in this section.

## Injection

When you annotate any property or entity within a graphql schema with @Resolver, Viaduct will generate code to act as an abstract class for that resolver.  It’s then up to you to implement that class, and provide instances of it to be used by Viaduct when the resolver is called by a graphql engine..  Viaduct is designed to find such instances through dependency injection. This is defined by using this interface:

```kotlin
interface TenantCodeInjector {
    fun <T> getProvider(clazz: Class<T>): Provider<T>
}
```

By default (primarily for tests) Viaduct will assume that a zero-argument constructor will be available for any resolver you may need to write, and Viaduct will automatically construct a single instance of this class to be used whenever the resolver is called.  Note that this is not guaranteed to be thread-safe, so it is not recommended as your main approach.

Examples of using this dependency injection mechanism are available in the demo applications.
