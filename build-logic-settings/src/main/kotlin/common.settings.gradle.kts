pluginManagement {
    plugins {
        id("org.jetbrains.dokka") version "2.0.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
}

gradle.allprojects {
    group = "com.airbnb.viaduct"
}
