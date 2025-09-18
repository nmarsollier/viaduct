pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("../../build-logic-settings")
    includeBuild("../../build-logic")
}

plugins {
    id("common")
}

rootProject.name = "viaduct-codegen"

// Include only codegen module
includeNamed(":tenant:codegen", "../..")

