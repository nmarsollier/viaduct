rootProject.name = "hello-world"

pluginManagement {
    plugins {
        id("viaduct-application") version "0.1.0"
        id("viaduct-module") version "0.1.0"
    }
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

include(":mymodule")
