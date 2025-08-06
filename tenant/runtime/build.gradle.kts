plugins {
    id("viaduct-feature-app")
}

viaductFeatureApp {
}

dependencies {
    implementation(project(":engine:engine-api"))
    implementation(project(":engine:engine-runtime"))
    implementation(project(":service:service-api"))
    implementation(project(":service:service-bootapi"))
    implementation(project(":shared:arbitrary"))
    implementation(project(":shared:graphql"))
    implementation(project(":shared:logging"))
    implementation(project(":shared:utils"))
    implementation(project(":tenant:tenant-api"))
    implementation(libs.classgraph)
    implementation(libs.graphql.java)
    implementation(libs.guice)
    implementation(libs.javax.inject)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines)

    testImplementation(testFixtures(project(":engine:engine-api")))
    testImplementation(testFixtures(project(":engine:engine-runtime")))
    testImplementation(testFixtures(project(":service:service-api")))
    testImplementation(testFixtures(project(":shared:graphql")))
    testImplementation(testFixtures(project(":tenant:tenant-api")))
    testImplementation(project(":service:service-runtime"))
    testImplementation(project(":tenant:testapps:fixtures"))
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.module)
    testImplementation(libs.kotest.property.jvm)
    testImplementation(libs.kotlinx.coroutines.core.jvm)
    testImplementation(libs.kotlinx.coroutines.jdk8)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.strikt.core)
}
