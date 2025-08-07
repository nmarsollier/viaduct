import org.gradle.internal.extensions.core.serviceOf

plugins {
    id("java")
    alias(libs.plugins.dependency.analysis)
}

val projectVersion = libs.versions.project
val groupId: String by project
group = groupId

val isMavenLocal = gradle.startParameter.taskNames.any { it.contains("publishToMavenLocal", true) }
val jarVersion = if (isMavenLocal) "${projectVersion.get()}-SNAPSHOT" else projectVersion.get()

configure<com.autonomousapps.DependencyAnalysisExtension> {
    issues {
        all {
            onUnusedDependencies {
                severity("fail")
            }
            onUsedTransitiveDependencies {
                severity("warn")
            }
            onIncorrectConfiguration {
                severity("warn")
            }
        }
    }

    dependencies {
        bundle("kotlin") {
            includeGroup("org.jetbrains.kotlin")
        }
    }
}

subprojects {
    tasks.withType<ProcessResources> {
        exclude("**/BUILD.bazel") // TODO: this seems useless
    }

    group = groupId
    version = jarVersion

    // TODO: can't directly apply it in all project because the "schema" projects need it too, but some other plugin won't allow adding anything in the build file there ...
    //  An exception occurred applying plugin request [id: 'viaduct-app']
    //  > Failed to apply plugin 'viaduct-app'.
    //   > Project ':schema' must not contain custom configuration in build.gradle.kts. Found: ...
    plugins.apply("kotlin-project")

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
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
