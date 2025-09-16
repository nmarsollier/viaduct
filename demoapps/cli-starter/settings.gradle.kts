rootProject.name = "hello-world"

pluginManagement {
    plugins {
        id("com.airbnb.viaduct.application-gradle-plugin") version "0.2.3"
        id("com.airbnb.viaduct.module-gradle-plugin") version "0.2.3"
    }
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            content {
                includeGroup("com.airbnb.viaduct")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
