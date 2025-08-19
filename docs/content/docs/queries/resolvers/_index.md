---
title: Resolvers
description: Understanding resolvers in Viaduct.
weight: 2
---

In Viaduct, *all* tenant code is provided in the form of either a *node resolver* or a *field resolver*.  You implement either kind of resolver by providing a subclass of a *resolver base class* that we generate for you.  Resolver base classes are best understood through example.  Let’s look at our simple schema again:

```graphql
type User implements Node {  
  id: ID!  
  firstName: String  
  lastName: String  
  displayName: String @resolver  
}
```

Note that we’ve added `@resolver` to the `displayName` field, which indicates that we want to provide a field-resolver for that field.

For this example, we generate two base classes, one for the node resolver for the `User` type, and one for the field resolver for the `User.displayName` field:

```kotlin
object Nodes {  
  abstract class User {  
    open suspend fun resolve(ctx: Context): viaduct.api.grts.User

    class Context: NodeExecutionContext<viaduct.api.grts.User>  
  }  
}

object UserResolvers {  
  abstract class DisplayName {  
    open suspend fun resolve(ctx: Context): String?

    class Context: FieldExecutionContext<User, NoArguments, NotComposite>  
  }  
}
```

(we’ll discuss the `Context` types in the sections below).

As a tenant developer, you provide the node resolver for `User` by writing a subclass of `Nodes.User` and the field resolver for `User.displayName` by writing a subclass of `UserResolvers.DisplayName`.  If your tenant module defines other node types, resolver base classes for those would also be defined in `NodeResolvers`, and if it asked for more field resolvers on the `User` type, base classes for those additional resolvers would be defined in `UserFieldResolvers`.

### ExecutionContext

When implementing a resolver, you have access to a number of parameters that are bundled into what we call *execution contexts*.  Both `Node-` and `FieldExecutionContext` implement the `ExecutionContext` base interface:

```kotlin
interface ExecutionContext {  
  suspend fun <T: Query> query(selections: SelectionSet<T>): T

  fun <T: Struct> builderFor(grt: KClass<T>): Builder<T>  
  fun <T: NodeObject> nodeFor(id: GlobalID<T>): T  
  fun <T: Object> globalIDFor(type: Type<T>, internalID: String): GlobalID<T>

  fun <T : CompositeOutput> selectionsFor(  
      type: Type<T>,  
      selections: @Selections String,  
      variables: Map<String, Any?> = emptyMap()  
  ): SelectionSet<T>  
}
```

The type-bounds found in this interface – for example, `Object` and `Struct` – are tagging-types for the GRTs of various flavors of GraphQL types – in our examples, GRTs for (`Object`) GraphQL object types and (`Struct`) GraphQL object and input types (i.e., types that have fields).  Here is what these functions do:

* `query` kicks off a subquery, i.e., a GraphQL query operation on the full “central schema” offered by Viaduct.
* `builderFor` returns a builder object for either a GraphQL object type or input type (`Struct` is a tagging interface for the GRTs of these two kinds of GraphQL types).
* `nodeFor` returns a “node reference,” which are described in their own subsection below.
* `globalIdFor` returns a global-identifier.
* `selectionsFor` constructs a selection set.  The `selections` argument is the text that is either a set of selections, or a fragment on type `T`.  (See  discussion on selection texts.)
 
## Selection Texts

In a number of places, notably in `@Resolver` annotation and the `ExecutionContext.selectionsFor` function, you are asked to provide “selections” on GraphQL types.  We support two different syntaxes for providing those selections.  Here are examples of selections on a hypothetical `User` type:

* Fragments:

  `fragment _ on User { displayName address { city } }`

  This syntax must conform to the named-fragment syntax of GraphQL.  (Note that any name can be used, not just `_` \- but it will be ignored.)

* Selection lists:

  `displayName address { city }`

  This syntax must conform to the *contents* of a named-fragment, i.e., the selections that come within the outer brackets of a fragment.

We support the fragment syntax because many IDEs and other tools can consume the central syntax and provide validation, syntax highlighting, field auto-complete, and other useful functions.  However, some prefer the shorter syntax.  At Airbnb we strongly recommend using the longer syntax in your application code, however inside tests the longer syntax can sometimes trip up some code processing tools (because they can’t find the right schema), so in that context the shorter syntax can be a workaround.

To indicate to these tools the language of selection texts, we have two annotations, `@GraphQL`, defined by IntelliJ, which is used in this context to mark selection text expressed as full fragments, and `@Selections`, defined by Viaduct, which is used in this context to 