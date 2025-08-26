plugins {
    id("kotlin-project")
    id("me.champeau.jmh").version("0.7.3")
    `java-test-fixtures`
    id("kotlin-static-analysis")
}

dependencies {
    api(libs.graphql.java)
    api(libs.jackson.annotations)
    api(libs.javax.inject)
    api(libs.kotlinx.coroutines.core.jvm)
    api(project(":engine:engine-api"))
    api(project(":service:service-api"))
    api(project(":shared:dataloader"))
    api(project(":shared:utils"))

    implementation(libs.caffeine)
    implementation(libs.checker.qual)
    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.slf4j.api)
    implementation(project(":shared:deferred"))
    implementation(project(":shared:graphql"))
    implementation(project(":shared:logging"))
    implementation(project(":snipped:errors"))
    implementation(project(":tenant:tenant-api"))

    testFixturesApi(libs.graphql.java)
    testFixturesApi(libs.kotest.property.jvm)
    testFixturesApi(libs.kotlinx.coroutines.core.jvm)
    testFixturesApi(project(":engine:engine-api"))
    testFixturesApi(project(":engine:engine-runtime"))
    testFixturesApi(project(":service:service-api"))
    testFixturesApi(project(":shared:arbitrary"))

    testFixturesImplementation(libs.caffeine)
    testFixturesImplementation(libs.checker.qual)
    testFixturesImplementation(libs.graphql.java.extension)
    testFixturesImplementation(libs.io.mockk.dsl)
    testFixturesImplementation(libs.io.mockk.jvm)
    testFixturesImplementation(libs.kotlinx.coroutines.jdk8)
    testFixturesImplementation(testFixtures(project(":service:service-api")))
    testFixturesImplementation(testFixtures(project(":service:service-api")))

    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jspecify)
    testImplementation(libs.kotest.property.jvm)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.strikt.core)
    testImplementation(project(":service:service-runtime"))
    testImplementation(project(":shared:arbitrary"))
    testImplementation(testFixtures(project(":engine:engine-api")))
    testImplementation(testFixtures(project(":engine:engine-runtime")))
    testImplementation(testFixtures(project(":service:service-api")))
    testImplementation(testFixtures(project(":shared:graphql")))

    jmh(libs.jmh.annotation.processor)

    jmhAnnotationProcessor(libs.jmh.annotation.processor)

    jmhApi(libs.jmh.core)
    jmhApi(project(":shared:arbitrary"))

    jmhImplementation(libs.graphql.java)
    jmhImplementation(libs.kotest.property.jvm)
    jmhImplementation(libs.kotlinx.coroutines.core.jvm)
    jmhImplementation(libs.kotlinx.coroutines.jdk8)
    jmhImplementation(project(":engine:engine-runtime"))
    jmhImplementation(testFixtures(project(":engine:engine-runtime")))
}
