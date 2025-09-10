---
title: Getting Started
description: Setting up and modifying a simple Viaduct application
weight: 1
---

## Getting Started with Viaduct

Using a set of scripts bundled with the Viaduct release, this document will walk you through the process of running a very simple Viaduct application.  Viaduct comes with built-in, Gradle-based tooling for building, testing, and running your sample application.  This guide uses that tooling and assumes a basic familiarity with Gradle.

### System Requirements

Java 21 must be on the path or available via JAVA_HOME.

### Running the Simple Application

Viaduct comes with a number of demonstration applications you can find in the `demoapps` directory.  Let’s start with the `demoapps/clihelloworld` application.  Change into that directory and type:

```shell
./gradlew -q run --args="'{ author }'"
```

This command will build the simple application run a graphql query with Viaduct.

### Going further

There are two other demo applications:

- [springhelloworld](demoapps/springhelloworld/README.md) - a simple integration of spring with Spring.
- [starwars](demoapps/starwars/README.md) - a full-fledged spring application to demonstrate more complex usage of
  Viaduct.
