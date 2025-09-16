import com.vanniktech.maven.publish.GradlePublishPlugin
import org.gradle.kotlin.dsl.get

plugins {
    `kotlin-dsl`
    id("kotlin-project")
    id("kotlin-static-analysis")
    id("com.gradle.plugin-publish") version "2.0.0"
    id("com.vanniktech.maven.publish")
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(project(":common"))

    // Your runtime helpers used by the plugin implementation (keep as needed)
    implementation("com.airbnb.viaduct:tenant-codegen")
    implementation("com.airbnb.viaduct:shared-graphql")

    // Do NOT leak the Kotlin Gradle Plugin at runtime
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
}

// Manifest with Implementation-Version for runtime access if you need it
tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version.toString()
        )
    }
}

gradlePlugin {
    website = "https://airbnb.io/viaduct"
    vcsUrl = "https://github.com/airbnb/viaduct"

    plugins {
        create("viaductModule") {
            // e.g., com.airbnb.viaduct.module-gradle-plugin
            id = "$group.module-gradle-plugin"
            implementationClass = "viaduct.gradle.ViaductModulePlugin"
            displayName = "Viaduct Module Plugin"
            description = "Module plugin for Viaduct module projects."
            tags.set(listOf("viaduct", "graphql", "kotlin"))
        }
    }
}

publishing {
    publications.withType<MavenPublication> {
        versionMapping {
            usage("java-api") { fromResolutionOf("runtimeClasspath") }
            usage("java-runtime") { fromResolutionResult() }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    // Publish this subproject as its own artifact coordinate
    // Resulting artifactId will be "module-gradle-plugin"
    configure(GradlePublishPlugin())
    coordinates(group as String, "module-gradle-plugin", version.toString())

    pom {
        name.set("Viaduct Module Gradle Plugin")
        description.set("Gradle plugin for Viaduct module projects.")
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
