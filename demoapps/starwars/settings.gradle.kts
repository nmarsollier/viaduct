rootProject.name = "starwars"

pluginManagement {
    includeBuild("../../plugins")
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

plugins {
    id("viaduct-settings") version "0.1.0"
}

viaduct.viaductInclude()

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

include(":schema")
include(":tenants:starwars")
include(":tenants:starships")
