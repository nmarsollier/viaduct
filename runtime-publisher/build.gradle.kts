import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64

plugins {
    id("java")
    id("maven-publish")
    signing
    jacoco
}

val projectDependencies: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = true
}

val nonTransitiveProjectDependencies: Configuration by configurations.creating {
    isTransitive = false
    extendsFrom(projectDependencies)
}

val testFixturesProjectDependencies: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
}

val projectNames = setOf(
    ":engine:engine-api",
    ":engine:engine-runtime",
    ":service:service-api",
    ":service:service-runtime",
    ":service:service-wiring",
    ":shared:arbitrary",
    ":shared:graphql",
    ":shared:logging",
    ":shared:utils",
    ":tenant:tenant-api",
    ":tenant:tenant-codegen",
    ":tenant:tenant-runtime",
    ":shared:arbitrary",
    ":shared:dataloader",
    ":shared:deferred",
    ":shared:graphql",
    ":shared:shared-codegen",
    ":shared:invariants",
    ":shared:logging",
    ":shared:viaductschema",
    ":snipped:errors",
)

val projectsToPackage = rootProject.subprojects.filter { it.path in projectNames }

dependencies {
    projectsToPackage.forEach {
        projectDependencies(project(it.path))
    }
}

gradle.projectsEvaluated {
    dependencies {
        projectsToPackage.forEach { sub ->
            when {
                sub.configurations.findByName("testFixtures") != null -> {
                    testFixturesProjectDependencies(
                        project(mapOf("path" to sub.path, "configuration" to "testFixtures"))
                    )
                }

                sub.configurations.findByName("testFixturesRuntimeElements") != null -> {
                    testFixturesProjectDependencies(
                        project(mapOf("path" to sub.path, "configuration" to "testFixturesRuntimeElements"))
                    )
                }
            }
        }
    }
}

@CacheableTask
abstract class UnpackProjectDependenciesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val artifacts: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    abstract val archives: ArchiveOperations

    @TaskAction
    fun unpack() {
        val outDir = outputDir.get()

        fs.delete { delete(outDir) }

        artifacts.files.forEach { jar ->
            fs.copy {
                from(archives.zipTree(jar))
                into(outDir)
            }
        }
    }
}

val unpackProjectDependencies by tasks.registering(UnpackProjectDependenciesTask::class) {
    artifacts.from(nonTransitiveProjectDependencies.incoming.artifacts.artifactFiles)
    artifacts.from(testFixturesProjectDependencies.incoming.artifacts.artifactFiles)
    outputDir = layout.buildDirectory.dir("unpacked-project-dependencies")
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(unpackProjectDependencies.flatMap { it.outputDir })
}

val aggregateSourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    archiveBaseName.set("runtime")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    projectsToPackage.forEach { sub ->
        val hasSourcesJar = sub.tasks.findByName("sourcesJar") != null
        if (hasSourcesJar) {
            @Suppress("UNCHECKED_CAST")
            val sj = sub.tasks.named("sourcesJar") as TaskProvider<Jar>
            dependsOn(sj)
            from(sj.map { zipTree(it.archiveFile.get().asFile) })
        } else {
            from(sub.layout.projectDirectory.dir("src/main/java")) { include("**/*.java") }
            from(sub.layout.projectDirectory.dir("src/main/kotlin")) { include("**/*.kt") }
        }
    }
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    // By not specifying 'from()', the JAR will be empty.
    // This task will create an empty JAR named like 'your-project-name-version-javadoc.jar'
}

configurations.apiElements {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements::class.java, LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.SHADOWED))
    }
}

val sonatypeUsername: String? by project
val sonatypePassword: String? by project

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "runtime"
            version = project.version.toString()
            artifact(tasks.jar.get())
            artifact(aggregateSourcesJar.get())
            artifact(emptyJavadocJar.get())

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

            pom.withXml {
                val depsNode = asNode().appendNode("dependencies")
                projectsToPackage.forEach { sub ->
                    sub.configurations.findByName("implementation")?.allDependencies
                        ?.filterIsInstance<ExternalModuleDependency>()
                        ?.forEach { dep ->
                            val depNode = depsNode.appendNode("dependency")
                            depNode.appendNode("groupId", dep.group)
                            depNode.appendNode("artifactId", dep.name)
                            depNode.appendNode("version", dep.version)
                            depNode.appendNode("scope", "compile")
                        }
                }
            }

            pom {
                name.set("Viaduct")
                description.set("A GraphQL-based microservice alternative.")
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

signing {
    sign(publishing.publications["maven"])
}

tasks.withType<Sign>().configureEach {
    onlyIf {
        !gradle.startParameter.taskNames.any { it.contains("publishToMavenLocal") }
    }
}

tasks.register("publishSonatypeDeployment") {
    doLast {
        val uploadUrl = "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/com.airbnb?publishing_type=automatic"

        val username = System.getenv("SONATYPE_USERNAME") ?: sonatypeUsername
        val password = System.getenv("SONATYPE_PASSWORD") ?: sonatypePassword

        val url = URI(uploadUrl).toURL()
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")

            val token = Base64.getEncoder().encodeToString("$username:$password".toByteArray(Charsets.UTF_8))

            connection.setRequestProperty("Authorization", "Basic $token")

            connection.doOutput = true

            val responseCode = connection.responseCode
            val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

            project.logger.debug("Response Code: $responseCode")
            project.logger.debug("Response: $responseMessage")

            if (responseCode != 200) {
                throw GradleException("Failed to publish sonatype deployment: $responseMessage")
            }
        } catch (e: Exception) {
            throw GradleException("Failed to upload: ${e.message}", e)
        } finally {
            connection.disconnect()
        }
    }
}
