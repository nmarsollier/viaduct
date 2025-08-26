import org.gradle.internal.extensions.core.serviceOf
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("java")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlintPlugin)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.dokka)
}

val projectVersion = libs.versions.project
val groupId: String by project
group = groupId

val jarVersion = projectVersion.get()

val ktlintCliVersion = libs.versions.ktlintVersion
val junitVersion = libs.versions.junit5
val junitLib = libs.junit
val kotlinTestLib = libs.kotlin.test


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
    apply(plugin = "org.jetbrains.dokka")
    
    // Disable Dokka for test apps and demo apps to avoid dependency issues
    if (project.path.contains("testapps") || 
        project.path.startsWith(":demoapps")) {
        tasks.named("dokkaHtmlPartial") {
            enabled = false
        }
    }
    
    tasks.withType<ProcessResources> {
        exclude("**/BUILD.bazel")
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

tasks.register("cleanBuildAndPublish") {
    outputs.upToDateWhen { false }
    notCompatibleWithConfigurationCache("Uses exec and Gradle subprocesses")

    doLast {
        val execOperations = project.serviceOf<ExecOperations>()
        execOperations.exec { commandLine("./gradlew", ":runtime:publishToMavenLocal", "--no-configuration-cache", "--no-scan") }
    }
}
