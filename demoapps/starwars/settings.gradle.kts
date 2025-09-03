rootProject.name = "starwars"

pluginManagement {
    plugins {
        id("viaduct-app") version "0.1.0"
        id("viaduct-schema") version "0.1.0"
        id("viaduct-tenant") version "0.1.0"
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

include(":schema")
include(":tenants:starwars")
include(":tenants:starships")
