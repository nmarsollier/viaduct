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

    // Not needed directly, but this dependency drags it into the
    // fat-jar for the gradle plugin
    // TODO: make plugin a regular subproject and then this
    // becomes a direct dependency of the plugin
    implementation(project(":shared:graphql"))

    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.javassist)
}

group = "com.airbnb.viaduct"
version = libs.versions.project.get()


tasks.jar {
    archiveClassifier.set("slim")
}

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    // ^^ improve the reproducibility of JARs
}



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
