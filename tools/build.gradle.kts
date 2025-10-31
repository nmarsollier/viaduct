plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
    id("conventions.kotlin-static-analysis")
    id("conventions.kotlin")
    signing
}

dependencies {
    implementation(libs.graphql.java)
    implementation(libs.clikt.jvm)
}

tasks.register<JavaExec>("validateSchema") {
    group = "verification"
    description = "Runs the schema validator"
    workingDir = File(System.getProperty("user.dir"))
    classpath = sourceSets["main"].runtimeClasspath

    mainClass.set("viaduct.cli.validation.schema.ViaductSchemaValidatorCLIKt")
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(emptyJavadocJar.get())

            pom {
                name.set("Viaduct Tools")
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
    }
}

signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    setRequired {
        gradle.taskGraph.allTasks.any { it is PublishToMavenRepository }
    }
    sign(publishing.publications["mavenJava"])
}
