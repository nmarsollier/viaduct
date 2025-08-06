rootProject.name = "hello-world"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

plugins {
    id("viaduct-settings") version "0.1.0-SNAPSHOT"
}

viaduct.viaductInclude()

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
