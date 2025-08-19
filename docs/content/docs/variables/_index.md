---
title: Variables
description: Using variables in Viaduct resolvers.
weight: 2
---

The fragments in `@Resolver` annotations can contain variables.  These variables can be bound to values in one of two ways:

1. Via the `variables` parameter in `@Resolver`
2. Via the resolver’s variable provider

#### @Resolver variables Parameter

Variables may be bound using the `variables` parameter of `@Resolver`, which is an array of `@Variable` annotations.  For example, consider this Resolver configuration for the field `MyType.foo`: that takes an argument `include`:

```kotlin
@Resolver(  
    """  
    fragment _ on MyType {  
        field @include(if: ${'$'}shouldInclude)  
    }  
    """,  
    variables = [Variable("shouldInclude", fromArgument = "includeMe")]  
)
```

This resolver fragment uses an `shouldInclude` variable. At runtime, the value for this variable will be determined by starting with the value of the `includeMe` argument to `MyType.foo`.  To support nested GraphQL input types, the `fromArgument` string can contain a dot-separate path.

There are two mutually-exclusive parameters to the `@Variable` that can be used to set the value of a variable: the `fromArgument` parameter just illustrated, and the `fromField` parameter, which takes a dot-separated path relative to the `objectValue` of an execution.  In the `fromField` case, the selection provided as the value of the `fromField` parameter is automatically inserted into the resolver’s required selection set.

#### Variable Provider

The `variables` parameter does not allow arbitrarily-computed values to be passed as variables.  To support more dynamic use cases, a variable-provider can be used.  Let’s say the required selection-set of the resolver for `MyType.foo` includes variables named `startDate` and `endDate`.  To provide dynamically-computed values for those variables, in your resolver subclass for `MyTypeResolvers.Foo` you add a nested class that implements `VariableProvider` to your resolver class:

```kotlin
@Variables(types = "startDate: Date, endDate: Date")  
class Vars : VariablesProvider<MyType_Foo_Arguments> {  
    override suspend fun provide(args: MyType_Foo_Arguments) =  
        LocalDate.now().let {  
            mapOf(  
                "startDate" to it,
                "endDate" to it.plusDays(7)  
            )  
        }  
    }
}
```

The value of the `types` parameter to `@Variables` must conforms to *VariableDefinitionlist* from [GraphQL Spec](https://spec.graphql.org/draft/#sec-Language.Variables). The `args` parameter to the `provide` function is the arguments of the field whose resolver class defines this variable provider, or `NoArguments` if the field takes no arguments.

See [WishlistParameterizedAvailableExperienceIds](https://git.musta.ch/airbnb/treehouse/blob/master/projects/viaduct/modules/data/wishlist/src/main/kotlin/com/airbnb/viaduct/data/wishlist/resolvers/WishlistParameterizedAvailableExperienceIdsResolver.kt) for an example.

### GlobalIDs

GlobalIDs are objects in Viaduct that contain 'type' and 'internalID' properties. They are used to uniquely identify node objects in the graph. GlobalID values support structural equality, as opposed to referential equality.

Viaduct Modern uses two different Kotlin types to represent `ID`s. Under two conditions will `GlobalID<T>` be generated, elsewhere, the type will simply be a String.

1. `id` field of a `Node` object type
2. A field of type `ID` with the `@idOf(type:"T")` directive, (where T must be a GraphQL object, interface, or union type)

Elsewhere, String will be used for IDs.

For the examples below, `id`, `id3` and `f2` are GlobalIDs and while `id2` and `f1` are Strings.

```graphql
type MyNode implements Node {
    id: ID!
    id2: ID!
    id3: ID! @idOf(type:"MyNode")
}

input Input {
    f1: ID!
    f2: ID! @idOf(type: "MyNode")
}
```

If a Node object type implements an interface, and that interface has an id field, then that interface must also implement Node.

Instances of GlobalID can be created using execution-context objects, e.g., `ExecutionContext.nodeIDFor(User, 123)`.

### Node References

GraphQL resolvers often need to link to another `Node`; for example, the `Listing.host` field might have node type `User`.  In this case, the service call that backs the referring node (the service for `Listing`s in our example) will typically have an *id* for the other node and needs to inform Viaduct that this id should be used to resolve the other node (the `User` in our example).

This is achieved by called `Context.nodeFor` to obtain a *node reference.*  The `nodeFor` function takes a global id for a node and returns (in our example) a special GRT for that node.  In our example, if this is the schema for `Listing`:

```graphql
type Listing implements Node {  
    id: ID!  
    hostId: ID! @idOf(type: “User”)  
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

This example illustrates a subtle aspect of the “responsibility set” of resolvers, which is that the “responsibility” for resolving a field that has node type is split between the resolver whose responsibility set contains the field, and the node resolver.  Specifically, the containing resolver is responsible for returning a node reference as illustrated here, effectively resolver the node’s `id` field.  From there, the node-resolver takes over to resolve the rest of the node’s responsibility set.

We noted above that the GRT returned by `nodeFor` is “special.”  It’s special because, in the code that calls `nodeFor`, *only* the `id` field is set; all other fields are not set an will throw an exception on an attempt to read them.  If for some reason a resolver needs a *resolved* node rather than a node reference, the resolver can use a subquery, which are described in the next subsection.
