pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("plugins")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
}

// TODO: figure out how to remove the build-scan stuff from external repo
plugins {
    // Our internal gradle enterprise deployment can't handle a higher version
    id("com.gradle.develocity").version("3.19.2")
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

develocity {
    server = "https://gradle-enterprise.musta.ch"
    buildScan {
        publishing.onlyIf {
            it.buildResult.failures.isNotEmpty()
        }
    }
}

rootProject.name = "viaduct"

includeNamed("runtime-publisher")

includeNamed("engine:api")
includeNamed("engine:runtime")
includeNamed("service")
includeNamed("service:api")
includeNamed("service:bootapi")
includeNamed("service:runtime")
includeNamed("tenant:api")
includeNamed("tenant:codegen")
includeNamed("tenant:runtime")
includeNamed("shared:codegen")

include("shared:utils")
include("shared:logging")
include("shared:deferred")
include("shared:graphql")
include("shared:arbitrary")
include("shared:viaductschema")
include("shared:invariants")
include("shared:dataloader")
include("snipped:errors")

include("tenant:testapps:fixtures")
include("tenant:testapps:hotloading")
include("tenant:testapps:hotloading:tenants:tenant1")
include("tenant:testapps:hotloading:tenants:tenant2")
include("tenant:testapps:hotloading:schema")
include("tenant:testapps:policycheck")
include("tenant:testapps:policycheck:tenants:tenant1")
include("tenant:testapps:policycheck:schema")
include("tenant:testapps:resolver")
include("tenant:testapps:resolver:tenants:tenant1")
include("tenant:testapps:resolver:tenants:tenant2")
include("tenant:testapps:resolver:tenants:tenant3")
include("tenant:testapps:resolver:schema")
include("tenant:testapps:scopes")
include("tenant:testapps:scopes:tenants:tenant1")
include("tenant:testapps:scopes:schema")
include("tenant:testapps:schemaregistration")
include("tenant:testapps:schemaregistration:tenants:tenant1")
include("tenant:testapps:schemaregistration:tenants:tenant2")
include("tenant:testapps:schemaregistration:schema")

/**
 * Include a project with a given name that is different from the path.
 *
 * @param path The path to the project.
 * @param projectName The name to assign to the project. If null, the path will be used as the name replacing ":" with "-".
 */
fun includeNamed(
    path: String,
    projectName: String? = null
) {
    include(path)
    project(":$path").name = projectName ?: path.replace(":", "-")
}
