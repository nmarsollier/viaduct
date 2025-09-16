plugins {
    id("kotlin-project")
    id("test-feature-app")
    id("kotlin-static-analysis")
    `java-test-fixtures`
}

viaductFeatureApp {
}

dependencies {
    api(libs.graphql.java)
    api(libs.guice)
    api(libs.javax.inject)
    api(project(":engine:engine-api"))
    api(project(":service:service-api"))
    api(project(":tenant:tenant-api"))

    implementation(project(":shared:graphql"))
    implementation(project(":shared:utils"))
    implementation(libs.classgraph)
    implementation(libs.guava)
    implementation(libs.slf4j.api)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.core.jvm)

    testFixturesApi(project(":engine:engine-api"))
    testFixturesApi(project(":tenant:tenant-api"))

    testFixturesImplementation(project(":engine:engine-runtime"))
    testFixturesImplementation(project(":service:service-api"))
    testFixturesImplementation(testFixtures(project(":tenant:tenant-api")))
    testFixturesImplementation(libs.graphql.java)
    testFixturesImplementation(libs.io.mockk.jvm)
    testFixturesImplementation(libs.kotlin.reflect)
    testFixturesImplementation(libs.kotlinx.coroutines.core)

    testImplementation(testFixtures(project(":engine:engine-api")))
    testImplementation(testFixtures(project(":service:service-api")))
    testImplementation(testFixtures(project(":shared:graphql")))
    testImplementation(testFixtures(project(":tenant:tenant-api")))
    testImplementation(project(":engine:engine-runtime"))
    testImplementation(project(":service:service-runtime"))
    testImplementation(project(":service:service-wiring"))
    testImplementation(project(":shared:arbitrary"))
    testImplementation(project(":tenant:testapps:fixtures"))
    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.jackson.core)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.module)
    testImplementation(libs.kotest.property.jvm)
    testImplementation(libs.kotlinx.coroutines.jdk8)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.strikt.core)
    testImplementation(libs.micrometer.core)
}