rootProject.name = "hello-world"

pluginManagement {
    plugins {
        id("com.airbnb.viaduct.application-gradle-plugin") version "0.2.0-SNAPSHOT"
        id("com.airbnb.viaduct.module-gradle-plugin") version "0.2.0-SNAPSHOT"
    }
    repositories {
        // mavenLocal()
        gradlePluginPortal()
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        // mavenLocal()
        mavenCentral()
    }
}

include(":schema")
include(":modules:tenant1")
