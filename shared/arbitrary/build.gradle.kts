plugins {
    id("kotlin-project")
    id("kotlin-static-analysis")
}

tasks.withType<Test>().configureEach {
    jvmArgs = listOf("-Xmx4g")
}

dependencies {
    api(libs.graphql.java)
    api(libs.kotest.property.jvm)
    api(project(":shared:invariants"))
    api(project(":shared:viaductschema"))

    implementation(project(":engine:engine-api"))
    implementation(project(":shared:utils"))
    implementation(libs.kotest.common.jvm)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.core.jvm)
    implementation(libs.kotlinx.coroutines.test)

    testImplementation(libs.kotest.assertions.shared)
}
