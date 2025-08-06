import org.gradle.internal.extensions.core.serviceOf
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("java")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlintPlugin)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dependency.analysis)
}

val projectVersion = libs.versions.project
val groupId: String by project
group = groupId

val isMavenLocal = gradle.startParameter.taskNames.any { it.contains("publishToMavenLocal", true) }
val jarVersion = if (isMavenLocal) "${projectVersion.get()}-SNAPSHOT" else projectVersion.get()

val ktlintCliVersion = libs.versions.ktlintVersion
val junitVersion = libs.versions.junit5
val junitLib = libs.junit
val kotlinTestLib = libs.kotlin.test

repositories {
    mavenCentral()
}

plugins.withId("org.jetbrains.kotlin.jvm") {
    configure<KotlinJvmProjectExtension> {
        jvmToolchain(17)
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    ignoreFailures.set(true)
}

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
        exclude("**/BUILD.bazel")
    }

    repositories {
        mavenCentral()
    }

    group = groupId
    version = jarVersion

    plugins.apply("org.jetbrains.kotlin.jvm")
    plugins.apply("io.gitlab.arturbosch.detekt")
    plugins.apply("org.jlleitschuh.gradle.ktlint")

    plugins.withId("org.jetbrains.kotlin.jvm") {
        configure<KotlinJvmProjectExtension> {
            jvmToolchain(17)
        }

        dependencies {
            add("testImplementation", junitLib)
            add("testImplementation", kotlinTestLib)
            add("testImplementation", rootProject.libs.konsist)
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    detekt {
        source.setFrom("src/main/kotlin", "src/test/kotlin", "src/testFixtures/kotlin")
        config.setFrom(file("$rootDir/detekt.yml"))
        ignoreFailures = true
    }

    ktlint {
        version.set(ktlintCliVersion) // Specify the ktlint version
        enableExperimentalRules.set(true) // Optional: Enable experimental rules
        outputToConsole.set(true) // Optional: Output results to the console
        ignoreFailures.set(true)
    }
}

dependencies {
    detektPlugins(libs.detekt.formatting)
    testImplementation(libs.konsist)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<io.gitlab.arturbosch.detekt.Detekt>("detekt") {
    doFirst {
        println("ignoreFailures: $ignoreFailures")
    }
}

tasks.register("cleanBuildAndPublish") {
    outputs.upToDateWhen { false }
    notCompatibleWithConfigurationCache("Uses exec and Gradle subprocesses")

    doLast {
        val execOperations = project.serviceOf<ExecOperations>()
        execOperations.exec { commandLine("./gradlew", "clean") }
        execOperations.exec { commandLine("./gradlew", "publishToMavenLocal", "--no-configuration-cache") }
        execOperations.exec { commandLine("./gradlew", ":plugins:publishToMavenLocal") }
    }
}
