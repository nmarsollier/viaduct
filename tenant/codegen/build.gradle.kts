plugins {
    id("kotlin-static-analysis")
    id("viaduct-classdiff")
}

viaductClassDiff {
    schemaDiff("schema") {
        actualPackage.set("actuals.api.generated")
        expectedPackage.set("viaduct.api.grts")
        schemaResource("graphql/schema.graphqls")
    }
}

dependencies {
    implementation(project(":shared:invariants"))
    implementation(project(":shared:shared-codegen"))
    implementation(project(":shared:utils"))
    implementation(project(":shared:viaductschema"))
    implementation(project(":tenant:tenant-api"))
    implementation(project(":engine:engine-api"))
    implementation(libs.clikt.jvm)
    implementation(libs.graphql.java)
    implementation(libs.javassist)
    implementation(libs.kotlinx.metadata.jvm)

    testImplementation(libs.io.mockk.jvm)
}
