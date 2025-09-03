rootProject.name = "hello-world-java"

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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

include(":server")
include(":viaduct:schema")
include(":viaduct:tenants:tenant1")
