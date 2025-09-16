/**
 * Orchestration plugin that builds the entire workspace: root + included builds.
 *
 * Composite tasks:
 * - build  : root subprojects' :build + included builds' :build (fallback :help)
 * - check  : root subprojects' :check + included builds' :check (fallback :help)
 * - test   : root subprojects’ all Test tasks + included builds’ :test (fallback :help)
 * - publishToMavenLocal   : ONLY the configured roots & included builds
 * - publishToMavenCentral : ONLY the configured roots & included builds
 * - dokka  : all Dokka tasks in root subprojects
 * - jacoco : all per-module jacocoTestReport in root + root aggregated if present
 * - ci     : build + check + test + jacoco + dokka + (buildHealth where present)
 */

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.initialization.IncludedBuild
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.create

// --------------------------- Extension ---------------------------

abstract class OrchestrationExtension {
    /** Included build *names* to publish (e.g., "viaduct-core", "viaduct-codegen", "viaduct-gradle-plugins"). */
    abstract val publishIncludedBuilds: ListProperty<String>

    /** Root project paths to publish (e.g., listOf(":viaduct-bom")). */
    abstract val publishRootProjects: ListProperty<String>
}

val orchestration = extensions.create<OrchestrationExtension>("orchestration").apply {
    publishIncludedBuilds.convention(emptyList())
    publishRootProjects.convention(emptyList())
}

// --------------------------- Helpers -----------------------------

/** Collect tasks named [name] across all root subprojects without realizing everything. */
fun Project.tasksNamedInSubprojects(name: String): List<Any> =
    subprojects.map { sp -> sp.tasks.matching { it.name == name } }

/** Collect tasks of [type] across all root subprojects using task avoidance. */
fun <T : Task> Project.tasksOfTypeInSubprojects(type: Class<T>): List<Any> =
    subprojects.map { sp -> sp.tasks.withType(type) }

/** Matches tasks in subprojects whose names start with [prefix] (e.g., dokka*). */
fun Project.tasksStartingWithInSubprojects(prefix: String): List<Any> =
    subprojects.map { sp -> sp.tasks.matching { it.name.startsWith(prefix) } }

/** Tasks with any of [names] but only within the explicit [projectPaths] (no accidental whole-repo publish). */
fun Project.tasksNamedInProjects(projectPaths: List<String>, names: Set<String>): List<Any> =
    projectPaths.mapNotNull { p -> runCatching { project(p) }.getOrNull() }
        .map { proj -> proj.tasks.matching { it.name in names } }

/**
 * DO NOT probe for optional tasks (Gradle will fail later). Choose exactly one path per included build.
 * For build/check/test we always target lifecycle tasks; for publishing we target the conventional names.
 */
fun IncludedBuild.chosenTask(kind: String): String = when (kind) {
    "build" -> ":build"
    "check" -> ":check"
    "test"  -> ":test" // if absent, :check will still run tests; see fallback below
    "publishLocal"   -> ":publishToMavenLocal"
    "publishCentral" -> ":publishAllPublicationsToMavenCentralRepository"
    "buildHealth"    -> ":buildHealth"
    else -> ":help"
}

/** Turn chosen task paths into task refs; if you know some builds don’t have it, map them to ":help". */
fun includedBuildRefs(
    gradle: org.gradle.api.invocation.Gradle,
    kind: String,
    filterNames: Set<String>? = null,
    fallbackToHelp: Boolean = true
): List<Any> = gradle.includedBuilds
    .asSequence()
    .filter { filterNames == null || it.name in filterNames }
    .map { ib ->
        val path = ib.chosenTask(kind)
        // If we are calling test/check/build for *every* included build, fall back to :help for infra-only ones.
        if (fallbackToHelp && (ib.name.contains("build-logic") || ib.name.contains("settings"))) {
            ib.task(":help")
        } else {
            ib.task(path)
        }
    }
    .toList()

// --------------------------- build ----------------------------

tasks.register("build") {
    group = "build"
    description = "Builds all root subprojects and all included builds."

    dependsOn(tasksNamedInSubprojects("build"))
    // All included builds → use ':build' (safe lifecycle) or ':help' for infra
    dependsOn(includedBuildRefs(gradle, kind = "build", filterNames = null, fallbackToHelp = true))
}

// --------------------------- check ----------------------------

tasks.register("check") {
    group = "verification"
    description = "Runs all checks across root and included builds."

    dependsOn(tasksNamedInSubprojects("check"))
    dependsOn(includedBuildRefs(gradle, kind = "check", filterNames = null, fallbackToHelp = true))
}

// --------------------------- test -----------------------------

tasks.register("test") {
    group = "verification"
    description = "Runs all tests in root subprojects and in included builds."

    dependsOn(tasksOfTypeInSubprojects(Test::class.java))
    // For included builds use ':test'; for infra fall back to ':help'
    dependsOn(includedBuildRefs(gradle, kind = "test", filterNames = null, fallbackToHelp = true))
}

// ----------------------- publish: mavenLocal ---------------------

tasks.register("publishToMavenLocal") {
    group = "publishing"
    description = "Publishes selected roots & included builds to mavenLocal."

    val rootTargets = orchestration.publishRootProjects.get()
    if (rootTargets.isNotEmpty()) {
        dependsOn(tasksNamedInProjects(rootTargets, setOf("publishToMavenLocal")))
    }

    val ibTargets = orchestration.publishIncludedBuilds.get().toSet()
    if (ibTargets.isNotEmpty()) {
        dependsOn(includedBuildRefs(gradle, kind = "publishLocal", filterNames = ibTargets, fallbackToHelp = false))
    }
}

// ----------------------- publish: Maven Central ------------------

tasks.register("publishToMavenCentral") {
    group = "publishing"
    description = "Publishes selected roots & included builds to Maven Central."

    val rootTargets = orchestration.publishRootProjects.get()
    if (rootTargets.isNotEmpty()) {
        // Vanniktech creates 'publishAllPublicationsToMavenCentralRepository'; some projects may also expose 'publishToMavenCentral'
        dependsOn(tasksNamedInProjects(rootTargets, setOf(
            "publishAllPublicationsToMavenCentralRepository",
            "publishToMavenCentral"
        )))
    }

    val ibTargets = orchestration.publishIncludedBuilds.get().toSet()
    if (ibTargets.isNotEmpty()) {
        dependsOn(includedBuildRefs(gradle, kind = "publishCentral", filterNames = ibTargets, fallbackToHelp = false))
    }
}

// ----------------------------- Dokka -----------------------------

tasks.register("dokka") {
    group = "documentation"
    description = "Runs all Dokka tasks in root subprojects."
    dependsOn(tasksStartingWithInSubprojects("dokka"))
}

// ----------------------------- JaCoCo ----------------------------

tasks.register("jacoco") {
    group = "verification"
    description = "Runs all per-module jacocoTestReport + the root aggregated report if present."
    dependsOn(tasksNamedInSubprojects("jacocoTestReport"))
    dependsOn(tasks.matching { it.name == "testCodeCoverageReport" })
}

// ------------------------------- CI --------------------------------

tasks.register("ci") {
    group = "verification"
    description = "CI entrypoint: build + check + test + jacoco + dokka + buildHealth (where present)."

    dependsOn("build", "check", "test", "jacoco", "dokka")

    // Root buildHealth if present
    tasks.findByName("buildHealth")?.let { dependsOn(it) }

    // Included builds' buildHealth for the *published* set (keeps noise down)
    val ibTargets = orchestration.publishIncludedBuilds.get().toSet()
    if (ibTargets.isNotEmpty()) {
        dependsOn(includedBuildRefs(gradle, kind = "buildHealth", filterNames = ibTargets, fallbackToHelp = true))
    }
}
