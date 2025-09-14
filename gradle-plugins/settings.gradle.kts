@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    repositories {
        gradlePluginPortal()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

includeBuild("..") {
    dependencySubstitution {
        substitute(module("com.airbnb.viaduct:tenant-codegen")).using(project(":tenant:tenant-codegen"))
    }
}