plugins {
    id("kotlin-project")
    id("kotlin-static-analysis")
    id("test-classdiff")
    `maven-publish`
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

    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.javassist)
}

group = "com.airbnb.viaduct"
version = libs.versions.project.get()