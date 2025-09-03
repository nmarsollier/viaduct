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

/*includeBuild("../..") {
    dependencySubstitution {
        substitute(module("com.airbnb.viaduct:runtime")).using(project(":runtime"))
    }
}*/ // TODO: can't figure out why this breaks schema generation in demo apps....

include(":schema")
include(":tenants:helloworld")
