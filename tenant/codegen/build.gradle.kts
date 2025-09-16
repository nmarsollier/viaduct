plugins {
    id("kotlin-project")
    id("kotlin-static-analysis")
    `maven-publish`
}


dependencies {
    implementation(libs.clikt.jvm)
    implementation(libs.graphql.java)
    implementation(libs.kotlinx.metadata.jvm)
    implementation(libs.viaduct.shared.invariants)
    implementation(libs.viaduct.shared.codegen)
    implementation(libs.viaduct.shared.utils)
    implementation(libs.viaduct.shared.viaductschema)

    runtimeOnly(libs.viaduct.tenant.api)

    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.javassist)
}
