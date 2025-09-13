rootProject.name = "hello-world"

pluginManagement {
    plugins {
        id("com.airbnb.viaduct.application-gradle-plugin") version "0.1.0"
        id("com.airbnb.viaduct.module-gradle-plugin") version "0.1.0"
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
include(":modules:tenant1")
