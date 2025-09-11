plugins {
    `java-test-fixtures`
    id("kotlin-project")
    id("kotlin-static-analysis")
}

dependencies {
    api(libs.graphql.java)
    api(libs.guice)
    api(libs.javax.inject)
    api(project(":shared:utils"))

    implementation(libs.caffeine)
    implementation(libs.checker.qual)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":shared:graphql"))
    implementation(project(":snipped:errors"))

    testFixturesApi(libs.graphql.java)
    testFixturesApi(project(":engine:engine-runtime"))
    testFixturesApi(project(":service:service-runtime"))
    testFixturesApi(project(":service:service-wiring"))
    testFixturesApi(testFixtures(project(":engine:engine-runtime")))

    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(project(":service:service-api"))
    testFixturesImplementation(testFixtures(project(":service:service-api")))
    testFixturesImplementation(testFixtures(project(":shared:dataloader")))
    testFixturesImplementation(testFixtures(project(":shared:graphql")))

    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.junit)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":engine:engine-runtime"))
    testImplementation(testFixtures(project(":engine:engine-runtime")))
}
