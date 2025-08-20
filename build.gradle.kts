import org.gradle.internal.extensions.core.serviceOf

plugins {
    id("dependency-analysis")
}

val projectVersion = libs.versions.project
val groupId: String by project
group = groupId

val isMavenLocal = gradle.startParameter.taskNames.any { it.contains("publishToMavenLocal", true) }
val jarVersion = if (isMavenLocal) "${projectVersion.get()}-SNAPSHOT" else projectVersion.get()

subprojects {
    group = groupId
    version = jarVersion
}

tasks.register("cleanBuildAndPublish") { // TODO: this is full of hacks... why is that?
    outputs.upToDateWhen { false }
    notCompatibleWithConfigurationCache("Uses exec and Gradle subprocesses")

    doLast {
        val execOperations = project.serviceOf<ExecOperations>()
        execOperations.exec { commandLine("./gradlew", "clean") }
        execOperations.exec { commandLine("./gradlew", "publishToMavenLocal", "--no-configuration-cache") }
        execOperations.exec { commandLine("./gradlew", ":plugins:publishToMavenLocal") }
    }
}
