rootProject.name = "hello-world-java"

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

include(":server")
viaduct.viaductInclude("viaduct")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
