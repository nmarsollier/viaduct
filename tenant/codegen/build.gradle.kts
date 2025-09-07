plugins {
    id("kotlin-project")
    id("kotlin-static-analysis")
    id("test-classdiff")
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
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
    testImplementation(project(":engine:engine-api"))
}

afterEvaluate {
    // TODO: a hack for the sake of this dependency-analysis task...
    tasks.named("explodeCodeSourceTest") {
        dependsOn(tasks.named("generateSchemaDiffSchemaSchemaObjects"))
        dependsOn(tasks.named("generateSchemaDiffSchemaKotlinGrts"))
    }
}


// Need to publish this project to the maven local cache so we can
// build the plugins.  We want "fat" jars here to simplify the
// publication flow (ie, only need to publish tenant-codegen)

group = "com.airbnb.viaduct" // TODO - find a better home for this constant
version = libs.versions.project.get()

tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    // ^^ improve the reproducibility of JARs
}

publishing {
    publications {
        create<MavenPublication>("shadowFatJar") {
            artifact(tasks.named("shadowJar"))
        }
    }
}
