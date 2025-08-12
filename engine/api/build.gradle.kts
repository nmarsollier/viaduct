plugins {
    `java-test-fixtures`
    id("kotlin-static-analysis")
}

dependencies {
    implementation(libs.caffeine)
    implementation(libs.graphql.java)
    implementation(libs.guice)
    implementation(libs.javax.inject)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines)
    implementation(project(":shared:graphql"))
    implementation(project(":shared:utils"))
    implementation(project(":snipped:errors"))

    testFixturesImplementation(libs.graphql.java)
    testFixturesImplementation(libs.io.mockk.jvm)
    testFixturesImplementation(libs.kotlinx.coroutines)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(project(":engine:engine-runtime"))
    testFixturesImplementation(project(":service:service-api"))
    testFixturesImplementation(project(":service:service-runtime"))
    testFixturesImplementation(project(":shared:graphql"))
    testFixturesImplementation(testFixtures(project(":engine:engine-runtime")))
    testFixturesImplementation(testFixtures(project(":service:service-api")))
    testFixturesImplementation(testFixtures(project(":shared:dataloader")))
    testFixturesImplementation(testFixtures(project(":shared:graphql")))

    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.junit)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.net.bytebuddy)
    testImplementation(project(":engine:engine-runtime"))
    testImplementation(project(":service:service-api"))
    testImplementation(testFixtures(project(":engine:engine-runtime")))
}
