rootProject.name = "hello-world-java"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

plugins {
    id("viaduct-settings") version "0.1.0-SNAPSHOT"
}

include(":server")
viaduct.viaductInclude("viaduct")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
