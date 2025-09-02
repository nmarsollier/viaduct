---
title: Field Resolvers
description: Writing resolvers for fields in Viaduct.
weight: 2
---

Let’s look at the resolver for `User.displayName`:

```kotlin
@Resolver(
    "fragment _ on User { firstName lastName }"
)
class UserDisplayNameResolver: UserResolvers.DisplayName() {
    override suspend fun resolve(ctx: Context): String? {
        var fn = ctx.objectValue.getFirstName()
        var ln = ctx.objectValue.getLastName()
        return when {
            fn == null && ln == null -> null
            fn == null -> ln
            ln == null -> fn
            else -> "$fn $ln"
        }
    }
}
```

As this example illustrates, in case of field resolvers, the `@Resolver` annotation can contain an optional fragment on the type that contains the field being resolved.  We call this fragment the *required selection set* of the resolver.  In this case, the required selection set asks for the `firstName` and `lastName` fields of `User`, which are combined to generate the display name of the user.  If a resolver attempts to access a field that’s not in its required selection set, an `UnsetSelectionException` is thrown at runtime.

**Important clarification:** there are no requirements on the names used for these resolver classes: We use `UserDisplayNameResolver` here as what would be a typical name, but that choice is not dictated by the framework.

Let’s walk through the members that `FieldExecutionContext` add to `ExecutionContext`:

```kotlin
interface FieldExecutionContext<T: Object, Q: Query, A: Arguments, O: CompositeOutput>: ExecutionContextt {
    val objectValue: T
    val queryValue: T
    val arguments: A
    fun selections(): SelectionSet<O>
}
```

* The `objectValue` property gives access to the object that contains the field being resolved.  Fields of that object can be accessed, but only if those fields are in the resolver’s required selection set.

* The `queryValue` property is similar to objectValue, but applies to the root query object of the Viaduct central schema. Like `objectValue`, fields on `queryValue` can only be accessed if they are in the resolver’s required selection set.

* The `arguments` property gives access to the arguments to the resolver.  When a field takes arguments, the Viaduct build system will generate a GRT representing the values of those arguments.  If `User.displayName` took arguments, for example, Viaduct would generate a type `User_DisplayName_Arguments` having one property per argument taken by `displayName`.  In our example, the field execution context for `displayName` is parameterized by the special type `NoArguments` indicating that the field takes no arguments.

* The `selections` function returns the selections being requested for this field in the query, same as the `selections` function for the node resolver.  The `SelectionSet` type is parameterized by the type of the selection set.  For example, in the case of `User`’s node resolver, `selections` returned `SelectionSet<User>`.  In the case of `displayName`, `selections` returns `SelectionSet<NotComposite>`, where the special type `NotComposite` indicates that `displayName` does not return a composite type (it returns a scalar instead).


