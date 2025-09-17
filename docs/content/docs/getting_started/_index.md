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

Viaduct comes with a number of demonstration applications you can find in the `demoapps` directory.  Let’s start with the `demoapps/cli-starter` application.  Change into that directory and type:

```shell
./gradlew -q run --args="'{ author }'"
```

This command will build the simple application run a graphql query with Viaduct.

## Using Viaduct in your own application

Currently Viaduct is only published to Maven Central as snapshot versions. To use Viaduct in your own application, add the following to your `build.gradle.kts` file:

```kotlin
plugins {
  id("com.airbnb.viaduct.application-gradle-plugin") version "0.3.0"
}

dependencies {
  implementation("com.airbnb.viaduct:runtime:0.3.0")
}
```

And add the following to your `settings.gradle.kts` file:

```kotlin
pluginManagement {
    plugins {
        id("com.airbnb.viaduct.application-gradle-plugin") version "0.3.0"
        id("com.airbnb.viaduct.module-gradle-plugin") version "0.3.0"
    }
}
```

### Going further

There are three demo applications:

- [cli-starter](https://github.com/airbnb/viaduct/blob/main/demoapps/cli-starter/) - the simplest possible starting point for a Viaduct application.
- [spring-starter](https://github.com/airbnb/viaduct/blob/main/demoapps/spring-starter/README.md) - a simple integration of Viaduct with Spring.
- [starwars](https://github.com/airbnb/viaduct/blob/main/demoapps/starwars/README.md) - a comprehensive demo of Viaduct's features
  Viaduct.
