pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("build-logic-settings")
    includeBuild("build-logic-commons")
    includeBuild("build-logic")
    includeBuild("build-test-plugins")

    plugins {
        id("org.jetbrains.dokka") version "2.0.0"
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
}

plugins {
    id("build-scans")
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "viaduct"

includeNamed("runtime-publisher", "runtime")

includeNamed("engine:api")
includeNamed("engine:runtime")
includeNamed("service")
includeNamed("service:api")
includeNamed("service:runtime")
includeNamed("service:wiring")
includeNamed("tenant:api")
includeNamed("tenant:codegen")
includeNamed("tenant:runtime")
includeNamed("shared:codegen")


include("tools")
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
include("tenant:testapps:policycheck")
include("tenant:testapps:policycheck:tenants:tenant1")
include("tenant:testapps:policycheck:schema")
include("tenant:testapps:resolver")
include("tenant:testapps:resolver:tenants:tenant1")
include("tenant:testapps:resolver:tenants:tenant2")
include("tenant:testapps:resolver:tenants:tenant3")
include("tenant:testapps:resolver:schema")
include("tenant:testapps:schemaregistration")
include("tenant:testapps:schemaregistration:tenants:tenant1")
include("tenant:testapps:schemaregistration:tenants:tenant2")
include("tenant:testapps:schemaregistration:schema")

include("docs")

include("gradle-plugins")

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
