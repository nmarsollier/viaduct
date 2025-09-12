plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
    id("kotlin-static-analysis")
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

    // You can also pass arguments to your main function
    // args("first-argument", "second-argument")
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
    repositories {
        mavenLocal()
    }
}
