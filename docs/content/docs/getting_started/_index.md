---
title: Getting Started
description: Setting up and modifying a simple Viaduct application
weight: 1
---

## Getting Started with Viaduct

Using a set of scripts bundled with the Viaduct release, this document will walk you through the process of setting up and modifying a very simple Viaduct application.  Viaduct comes with built-in, Gradle-based tooling for building, testing, and running your sample application.  This guide uses that tooling and assumes a basic familiarity with Gradle.

## System Requirements

Gradle 8+ and Java 21\.

## Installing Viaduct

Clone the Viaduct repository from GitHub and use Gradle to build and publish Viaduct's artifacts.

```shell
git@github.com:airbnb/viaduct.git
cd viaduct
./gradlew cleanBuildAndPublish
```

This will build Viaduct’s artifacts and install them into your local Maven repository.

Assuming you’re still in the `viaduct` directory, it will be convenient to

```shell
export VIADUCT="$(pwd)"
```

## Bootstrapping an Application

Viaduct comes with built-in Gradle tooling for building, testing, and publishing applications.  This tooling includes a “bootstrap” script that configures a working “Hello, World\!” application.

The first step is to create a basically blank, but functional, gradle project.  To do this, in an empty directory (which we’ll henceforth refer to as your “root directory”), type the following:

```shell
$VIADUCT/bootstrap-viaduct.sh
```

This should turn your empty directory into a Viaduct “Hello, World\!” application.  That script will also run a test query against it.  You can run a query yourself by typing:

```shell
./gradlew -q run --args="'{ author }'"
```

## Touring the Application

The bootstrap script created a Viaduct application project for you with the following shape:

```
yourroot/
  build.gradle.gts
  src/
    ...
  modules/
    helloworld/
      build.gradle.kts
      src/
        ...
```

(NOTE TO REVIEWERS: you’ll also see a directory called `schema`: as suggested below, this will be moved to `.viaduct/schema` to hide it.  Also what’s called `modules` above is currently named `tenants` and uses the `viaduct-tenant` plugin rather than the `viaduct-module` one.)

You can see that a viaduct application is a multi-project Gradle project:

The **root directory** of the application uses the `viaduct-app` Gradle plugin, telling gradle this directory contains a Viaduct application.  Note that the Gradle project in this directory does *not* need to be a Gradle root project: Viaduct apps can be embedded in larger code repositories.

The root project of our sample application has its own `src` directory: this directory contains the top-level “containing” application that sets up and runs the Viaduct application.  Right now, this application is a command-line application that runs queries passed to it as a CLI argument.  Later in this tutorial we will turn this into an actual web server.

Note that the containing application code does not need to be inside the `yourroot` directory: it can be in a sibling directory, for example, or someplace else altogether.  We’ll discuss this further below.

The **`modules` directory** contains the Viaduct modules containing the schema and business logic of your application.  Each of these modules needs to use the `viaduct-module` plugin.  Each of these modules should have a directory `src/resources/schema` containing `.graphqls` files with the schema being defined by that module.  (This `schema` directory is searched recursively for such schema-containing files.)

The `modules` directory can contain multiple Viaduct modules organized in an arbitrary directory structure.  At Airbnb, for example, this directory contains subdirectories named `entity`, `data`, and `presentation`, each representing a “layer” in our schema.  The GraphQL modules are put under these directories.  The `modules` directory can also contain “regular” Gradle projects, e.g., a `modules/common` project containing a library of common code shared by all modules.

A `viaduct-module` project may *not* contain other `viaduct-module` projects.  Also, `viaduct-module` projects cannot take Gradle dependencies on other `viaduct-module` projects: shared code *must* be put in a shared library, and the business logic of `viaduct-modules` should *only* interact using Viaduct GraphQL-based mechanisms.

At startup, the `viaduct-app` plugin will search for `viaduct-module` projects in the `modules` directory and automatically add them to the application-level project.  So to add a new module, you simply need to create a `viaduct-module` project for it someplace under `modules`.

(You might notice the directory `.viaduct/schema` in your application directory.  This is a Gradle project managed entirely by Viaduct: it’s used at build time to collect up the schema definitions across all Viaduct modules and aggregating them into a single, consolidated schema.  If you handcraft or write your own scripts to create Viaduct applications, this project must exist for the `viaduct-app` and `viaduct-schema` plugins to work correctly.

## Extending the Application

### Extending the Schema

Let’s explore our sample application more deeply by extending its functionality.  Viaduct is a “schema first” GraphQL environment, meaning you write your schema first, and then generate classes to write your code against.  So in that spirit, let’s start by extending the schema, which as noted above you’ll find in `src/main/resources/schema/schema.graphqls` (paths in this section are relative to `modules/helloworld/).`  You should see the following in that file:

```graphql
extend type Query @scope(to: ["publicScope"]) {
  greeting: String @resolver
  author: String @resolver
}
```

Viaduct itself has built-in definitions for the root GraphQL types `Query` and `Mutation`, although Viaduct doesn’t yet support subscriptions.  (As explained in our SRE guide, the `Mutation` type can be removed, and the names of both root types can be configured.)  Since `Query` is built-in, application code needs to extend it as illustrated above.  You’ll also see in this schema fragment that both fields have `@resolver` applied to them, meaning that a developer-provided function is needed to compute their respective value.  (*All* fields of `Query` must have `@resolver` applied to them.)

Let’s extend this schema to add a new field, `attributedGreeting`, which will attribute the greeting to its author:

```graphql
extend type Query @scope(to: ["publicScope"]) {
  greeting: String @resolver
  author: String @resolver
  attributedGreeting: AttributedGreeting @resolver
}

type AttributedGreeting {
  greeting: String
}
```

There’s no practical reason to have the `AttributedGreeting` type here: `attributedGreeting` could’ve just been a `String`.  We’re using a GraphQL object-type here in order to demonstrate some features of our API.

## Extending the code

The resolvers for our sample app have been put in `src/main/kotlin/com/example/viadapp/helloworld/HelloWorldResolvers.kt`.  We won’t copy its current content, but to support our new field apply the following changes to that file:

```kotlin
package com.example.viadapp.helloworld

import viaduct.api.Resolver
import com.example.viadapp.helloworld.resolverbases.QueryResolvers
import viaduct.api.grts.AttributedGreeting  // New import

// New code:
@Resolver("""
  greeting
  author
""")
class AttributedGreetingResolver : QueryResolvers.AttributedGreeting() {
    override suspend fun resolve(ctx: Context): AttributedGreeting {
        val greeting = ctx.objectValue.getGreeting()
        val author = ctx.objectValue.getAuthor()
        return AttributedGreeting.Builder(ctx)
            .greeting("$author says: \"$greeting\"")
            .build()
    }
}
```

As you make these changes you’ll notice two similar classes already in `HelloWorldResolvers.kt`,  `GreetingResolver` which contains the resolver for the `greeting` field and `AuthorResolver` for the `author` field.

We’ll dive into this file in some detail, but let’s summarize what our new resolver does.  The basic idea is that the resolver for `attributedGreeting` will combine the `author` and `greeting` fields into a string that attributes the greeting to the author.  The resolver has access to these two fields because its `@Resolver` annotation indicates that it needs those fields: if the `@Resolver` annotation didn’t mention the `author` field, for example, then the attempt to read `objectValue.author()` would fail at runtime.

Let’s examine some of the details of Viaduct that are illustrated by this file:

* To support code-generation, Viaduct requires that the code that makes up an application be placed in packages whose fully-qualified names (FQNs) share a common prefix, called the *application package prefix*, or app prefix for short.  This prefix can be anything; in our example, this prefix is `com.example.viadapp`.  This prefix is declared in the `build.gradle.kts` file of the `viaduct-app` project.

* Each module has a fully-qualified module name based on its path in the `modules` directory.  In the case of our hello-world module, since its Gradle project is found in `modules/helloworld`, its module name is `helloworld`.  If our application had a module in `modules/entity/users`, that module’s name would be `entity.users`.  The code that makes up a module should be placed in packages whose FQNs start with the concatenation of the app prefix and the module name, which is `com.example.viadapp.helloworld` in our example.  We call this the *module package prefix,* or *module prefix* for short.

* Viaduct generates code into two packages: `viaduct.api.grts` and `<moduleprefix>.resolverbases`:

  * The acronym GRT stands for *GraphQL Representational Type.*  These are generated Kotlin classes intended to represent GraphQL values.  In our code fragment above, we import the GRT for the `AttributedGreeting` class because our resolver for `attributedGreeting` will need to return an instance of one.

  * *Resolver classes* are classes that contain the application logic that should be executed to resolve the value of a field.  Each time a field is to be resolved, an instance of its resolver class is created (using an arbitrary dependency-injection framework if desired), and the resolver function in that class is then called.

    Resolver classes are implemented by subclassing a generated *resolver base class,* which are found in the `resolverbases` subpackage.  For each type `T` that has one or more `@resolver` fields, a static Kotlin `object` named `TResolvers` is created.  For each field `T.f` that has a resolver, we generate a resolver base class named `TResolvers.F` (where the first letter of `f` is capitalized).  To write a resolver for `T.f`, you subclass `TResolvers.F` and override the `resolve` function.  (This subclassing approach is friendly to IDE autocomplete and genAI automation.  Also, the type parameters on the `Context` argument are extensive, and this approach allows us to hide those from developers.)

* In addition to subclassing the correct resolver base class, resolver classes must also be annotated with `@Resolver`, otherwise they won’t be recognized as resolver classes.  (This supports testing and experimentation, where an “inactive” version of a resolver class with `@Resolver` can be written and tested before it replaces the current “live” version annotation.)
  As illustrated here, the `@Resolver` annotation can take what’s called a *required selection set* (RSS) which specifies what data the resolver wants to consume.  The RSS is a GraphQL fragment on the containing-type of the field being resolved.  In our example, since `attributedGreeting` is a field on `Query`, the RSS is a fragment on the `Query` type.  As mentioned earlier, if a resolver attempts to access data it hasn’t requested in its RSS, an error is raised.  Required selection sets are the *only* way the different modules of an application can interact with each other, creating strong encapsulation of the code inside a module.

* In all resolvers, the `ctx.objectValue` property is how you access the values requested in your RSS.  For a field on GraphQL type `T`, `objectValue` for the resolver for that field will be the GRT representing `T` (`Query` in our example).  Note that the getters for the fields of `objectValue` are functions, not properties.  It turns out these are suspending functions, which will wait for a value to be resolved by the responsible resolver before it returns.  This asynchrony allows the engine to execute resolvers in parallel.  (To prevent deadlocks, cycles between RSSs are checked-for at build time and also again at server startup time.)

