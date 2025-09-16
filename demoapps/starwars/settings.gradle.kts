rootProject.name = "viaduct-starwars-demoapp"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

val viaductGradlePluginVersion: String by settings

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
    versionCatalogs {
        create("libs") {
            // This injects a dynamic value that your TOML can reference.
            version("viaductGradlePluginVersion", viaductGradlePluginVersion)
        }
    }
}

include(":modules:starwars")
include(":modules:starships")
