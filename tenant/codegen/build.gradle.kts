plugins {
    id("kotlin-project")
    id("kotlin-static-analysis")
    id("test-classdiff")
    `maven-publish`
    id("com.gradleup.shadow") version "9.1.0" // TODO: move to catalog
}

viaductClassDiff {
    schemaDiff("schema") {
        actualPackage.set("actuals.api.generated")
        expectedPackage.set("viaduct.api.grts")
        schemaResource("graphql/schema.graphqls")
    }
}

dependencies {
    api(libs.clikt.jvm)
    api(libs.kotlinx.metadata.jvm)
    api(project(":shared:invariants"))
    api(project(":shared:shared-codegen"))
    api(project(":shared:utils"))
    api(project(":shared:viaductschema"))

    implementation(libs.graphql.java)
    implementation(project(":tenant:tenant-api"))

    testImplementation(libs.javassist)
    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
}

group = "com.airbnb.viaduct" // TODO - find a better home for this constant
version = libs.versions.project.get()



tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")
    configurations = setOf(project.configurations.shadow.get())
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    // ^^ improve the reproducibility of JARs
}

configurations.archives {
    outgoing.artifacts.clear()
}
configurations.apiElements {
    outgoing.artifacts.clear()
    outgoing.artifact(tasks.shadowJar)
}
configurations.runtimeElements {
    outgoing.artifacts.clear()
    outgoing.artifact(tasks.shadowJar)
}



// Publishing the fat jar (or some alternative with proper transitive dependencies) to an actual Maven repository (not local cache)
publishing {
    publications {
        create<MavenPublication>("shadow") {
            from(components["shadow"])
        }
    }
    repositories {
        // TODO: will be needed for publication to real Maven repositories
    }
}



afterEvaluate {
    // TODO: a hack for the sake of this dependency-analysis task...
    tasks.named("explodeCodeSourceTest") {
        dependsOn(tasks.named("generateSchemaDiffSchemaSchemaObjects"))
        dependsOn(tasks.named("generateSchemaDiffSchemaKotlinGrts"))
    }
}