import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.JavaPlatform
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

plugins {
    id("com.vanniktech.maven.publish")
}

// Simple extension users can configure in their module build files:
// viaductPublishing { artifactId.set("my-artifact-id") }
abstract class ViaductPublishingExtension @javax.inject.Inject constructor(
    objects: ObjectFactory
) {
    /** The artifactId to publish with. Defaults to project.name. */
    val artifactId: Property<String> = objects.property(String::class.java).convention("")
}

val viaductPublishing = extensions.create<ViaductPublishingExtension>("viaductPublishing")

mavenPublishing {
    publishToMavenCentral()

    // Sign only in CI by default (tweak as you like)
    if (providers.environmentVariable("CI").orElse("false").get().toBoolean()) {
        signAllPublications()
    }

    // If this is a Java Platform (BOM) project, use the JavaPlatform publishing
    if (plugins.hasPlugin("java-platform")) {
        configure(JavaPlatform())
    } else {
        // Default JVM coordinates & packaging for libraries
        configure(
            KotlinJvm(
                javadocJar = JavadocJar.None(), // keep as-is, change if you add Dokka
                sourcesJar = true,
            )
        )
    }

    // Resolve artifactId with a sensible default
    val resolvedArtifactId = viaductPublishing.artifactId
        .map { it.ifBlank { project.name } }
        .orElse(project.provider { project.name })
        .get()

    // group/version are the canonical Gradle props; stringify for safety
    coordinates(group.toString(), resolvedArtifactId, version.toString())

    pom {
        // You can still override these in the applying project if needed
        url.set("https://airbnb.io/viaduct/")
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

// Preserve your version mapping behavior (optional)
afterEvaluate {
    publishing {
        publications.withType(org.gradle.api.publish.maven.MavenPublication::class.java) {
            versionMapping {
                usage("java-api") { fromResolutionOf("runtimeClasspath") }
                usage("java-runtime") { fromResolutionResult() }
            }
        }
    }
}
