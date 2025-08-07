plugins {
    id("kotlin-static-analysis")
}

tasks.withType<Test>().configureEach {
    jvmArgs = listOf("-Xmx4g")
}

dependencies {
    implementation(project(":engine:engine-api"))
    implementation(project(":shared:invariants"))
    implementation(project(":shared:utils"))
    implementation(project(":shared:viaductschema"))
    implementation(libs.graphql.java)
    implementation(libs.kotest.common.jvm)
    implementation(libs.kotest.property.jvm)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.core.jvm)
    implementation(libs.kotlinx.coroutines.test)
}
