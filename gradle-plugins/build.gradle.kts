import org.gradle.kotlin.dsl.get

plugins {
    id("kotlin-project-without-tests")
    id("kotlin-static-analysis")
    id("build-logic-utils")
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.shadow)
    signing
}

group = "com.airbnb.viaduct" // TODO - don't hardwire this

project.version = libs.versions.project.get()
version = project.version

gradlePlugin {
    website = "https://airbnb.io/viaduct"
    vcsUrl = "https://github.com/airbnb/viaduct"

    plugins {
        create("viaductApplication") {
            id = "com.airbnb.viaduct.application-gradle-plugin"
            implementationClass = "viaduct.gradle.ViaductApplicationPlugin"
            displayName = "Viaduct Application Plugin"
            description = "Empty scaffold for the Viaduct application plugin."
            tags.set(listOf("viaduct", "graphql", "kotlin"))
        }
        create("viaductModule") {
            id = "com.airbnb.viaduct.module-gradle-plugin"
            implementationClass = "viaduct.gradle.ViaductModulePlugin"
            displayName = "Viaduct Module Plugin"
            description = "Empty scaffold for the Viaduct module plugin."
            tags.set(listOf("viaduct", "graphql", "kotlin"))
        }
    }
}

dependencies {
    implementation(project(":tenant:tenant-codegen"))
    implementation(project(":shared:graphql"))

    compileOnly(buildLogicUtils.plugin(libs.plugins.kotlin.jvm))
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}

java {
    withSourcesJar()
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    // By not specifying 'from()', the JAR will be empty.
    // This task will create an empty JAR named like 'your-project-name-version-javadoc.jar'
}

val sonatypeUsername: String? by project
val sonatypePassword: String? by project

publishing {
    publications {
        create<MavenPublication>("viaductPluginLib") {
            from(components["shadow"])
            artifactId = "gradle-plugins"
            version = project.version.toString()

            // Only needed when publishing to maven central
            artifact(emptyJavadocJar)

            pom {
                name.set("Viaduct Plugins")
                description.set("A GraphQL-based microservice alternative.")
                url.set("https://airbnb.io/viaduct/")

                organization {
                    name.set("Airbnb, Inc.")
                    url.set("https://github.com/airbnb")
                }

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("airbnb")
                        name.set("Airbnb, Inc.")
                        email.set("viaduct-maintainers@airbnb.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/airbnb/viaduct.git")
                    developerConnection.set("scm:git:ssh://github.com/airbnb/viaduct.git")
                    url.set("https://github.com/airbnb/viaduct")
                }
            }
        }
    }

    repositories {
        mavenLocal()
        maven {
            credentials {
                username = System.getenv("SONATYPE_USERNAME") ?: sonatypeUsername
                password = System.getenv("SONATYPE_PASSWORD") ?: sonatypePassword
            }
            name = "sonatype"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
        }
    }
}

// this is a hacky workaround
afterEvaluate {
    publishing {
        publications.all {
            (this as MavenPublication).pom {
                name.set("Viaduct Plugins")
                description.set("A GraphQL-based microservice alternative.")
                url.set("https://airbnb.io/viaduct/")

                organization {
                    name.set("Airbnb, Inc.")
                    url.set("https://github.com/airbnb")
                }

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("airbnb")
                        name.set("Airbnb, Inc.")
                        email.set("viaduct-maintainers@airbnb.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/airbnb/viaduct.git")
                    developerConnection.set("scm:git:ssh://github.com/airbnb/viaduct.git")
                    url.set("https://github.com/airbnb/viaduct")
                }
            }
        }
    }
}

afterEvaluate {
    tasks.named("publishPluginMavenPublicationToSonatypeRepository") {
        dependsOn("emptyJavadocJar")
        dependsOn("signViaductPluginLibPublication")
    }
    tasks.named("signPluginMavenPublication") {
        dependsOn("emptyJavadocJar")
    }
    tasks.named("signViaductPluginLibPublication") {
        dependsOn("javadocJar")
    }
    tasks.named("generateMetadataFileForPluginMavenPublication") {
        dependsOn("emptyJavadocJar")
    }
}

signing {
    sign(publishing.publications["viaductPluginLib"])
}

tasks.withType<Sign>().configureEach {
    onlyIf {
        !gradle.startParameter.taskNames.any { it.contains("publishToMavenLocal") }
    }
}

afterEvaluate {
    /* This has to be done afterEvaluate because the various publish
     * tasks are created dynamically by the maven-publish plugin
     */
    tasks.named("publishViaductPluginLibPublicationToSonatypeRepository") {
        dependsOn("signViaductPluginLibPublication")
    }
    tasks.named("publishViaductPluginLibPublicationToSonatypeRepository") {
        dependsOn("signPluginMavenPublication")
    }
    tasks.named("publishViaductPluginLibPublicationToMavenLocal") {
        dependsOn("signPluginMavenPublication")
    }
    tasks.named("publishPluginMavenPublicationToMavenLocalRepository") {
        dependsOn("signViaductPluginLibPublication")
    }
    tasks.named("publishViaductPluginLibPublicationToMavenLocalRepository") {
        dependsOn("signPluginMavenPublication")
    }
}


detekt {
    source.setFrom("src/main/kotlin", "src/test/kotlin")
    config.setFrom("$projectDir/../detekt.yml")
    ignoreFailures = true
}

ktlint {
    version.set("1.2.1")
    enableExperimentalRules.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(true)

    filter {
        exclude { element ->
            element.file.path.contains("/generated-sources/")
        }
    }
}
