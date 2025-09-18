pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("../build-logic-settings")
    includeBuild("../build-logic-root")
    includeBuild("../build-logic")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots")
        }
    }
}

plugins {
    id("common")
}

rootProject.name = "viaduct-gradle-plugins"

includeNamed(":common")
includeNamed(":application-plugin")
includeNamed(":module-plugin")
