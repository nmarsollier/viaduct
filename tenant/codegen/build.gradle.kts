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
    implementation(libs.clikt.jvm)
    implementation(libs.graphql.java)
    implementation(libs.kotlinx.metadata.jvm)
    implementation(project(":shared:invariants"))
    implementation(project(":shared:shared-codegen"))
    implementation(project(":shared:utils"))
    implementation(project(":shared:viaductschema"))
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

group = "com.airbnb.viaduct" // TODO - find a better home for this constant
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
