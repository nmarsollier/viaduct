import org.gradle.internal.extensions.core.serviceOf

plugins {
    id("dependency-analysis")
}

val projectVersion = libs.versions.project
val groupId: String by project
group = groupId

val jarVersion = projectVersion.get()

subprojects {
    group = groupId
    version = jarVersion
}

tasks.register("cleanBuildAndPublish") {
    outputs.upToDateWhen { false }
    notCompatibleWithConfigurationCache("Uses exec and Gradle subprocesses")

    doLast {
        val execOperations = project.serviceOf<ExecOperations>()
        execOperations.exec { commandLine("./gradlew", "clean") }
        execOperations.exec { commandLine("./gradlew", ":plugins:publishToMavenLocal", "--no-configuration-cache") }
        execOperations.exec { commandLine("./gradlew", ":runtime:publishToMavenLocal", "--no-configuration-cache", "--no-scan") }
    }
}
