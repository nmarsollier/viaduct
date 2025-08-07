plugins {
    kotlin("jvm")
    id("viaduct-feature-app")
    id("me.champeau.jmh").version("0.7.3")
    `java-test-fixtures`
    id("kotlin-static-analysis")
}

dependencies {
    jmh(libs.jmh.annotation.processor)
    jmh(libs.jmh.core)

    api(libs.slf4j.api)

    implementation(project(":engine:engine-api"))
    implementation(project(":service:service-api"))
    implementation(project(":shared:dataloader"))
    implementation(project(":shared:deferred"))
    implementation(project(":shared:graphql"))
    implementation(project(":shared:logging"))
    implementation(project(":shared:utils"))
    implementation(libs.caffeine)
    implementation(libs.graphql.java)
    implementation(libs.graphql.java.extension)
    implementation(libs.guice)
    implementation(libs.jackson.databind)
    implementation(libs.javax.inject)
    implementation(libs.jmh.core)
    implementation(libs.kotlinx.coroutines.core.jvm)
    implementation(libs.kotlinx.coroutines.jdk8)

    testFixturesImplementation(testFixtures(project(":service:service-api")))
    testFixturesImplementation(project(":engine:engine-api"))
    testFixturesImplementation(project(":service:service-api"))
    testFixturesImplementation(libs.graphql.java)
    testFixturesImplementation(libs.io.mockk.jvm)

    jmhAnnotationProcessor(libs.jmh.annotation.processor)

    testImplementation(testFixtures(project(":engine:engine-api")))
    testImplementation(testFixtures(project(":engine:engine-runtime")))
    testImplementation(testFixtures(project(":service:service-api")))
    testImplementation(testFixtures(project(":shared:graphql")))
    testImplementation(project(":service:service-bootapi"))
    testImplementation(project(":service:service-runtime"))
    testImplementation(project(":shared:arbitrary"))
    testImplementation(project(":shared:dataloader"))
    testImplementation(project(":snipped:errors"))
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.kotest.property.jvm)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.strikt.core)
    testImplementation(libs.strikt.jvm)
}
