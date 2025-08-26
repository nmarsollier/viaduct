rootProject.name = "hello-world"

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
