import viaduct.gradle.internal.includeNamed

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("../../build-logic")
}

plugins {
    id("settings.common")
}

rootProject.name = "viaduct-codegen"

// Include only codegen module
includeNamed(":tenant:codegen", "../..")

