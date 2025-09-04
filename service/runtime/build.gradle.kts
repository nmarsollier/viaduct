plugins {
    id("kotlin-project")
}

dependencies {
    api(project(":engine:engine-api"))
    api(project(":engine:engine-runtime"))
    api(project(":service:service-api"))
    api(libs.graphql.java)
    api(libs.guice)

    implementation(project(":shared:graphql"))
    implementation(project(":shared:utils"))
    implementation(libs.caffeine)
    implementation(libs.classgraph)
    implementation(libs.slf4j.api)
    implementation(libs.kotlinx.coroutines.core.jvm)
    implementation(libs.kotlinx.coroutines.jdk8)

    testImplementation(testFixtures(project(":engine:engine-api")))
    testImplementation(testFixtures(project(":shared:graphql")))
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.jspecify)
    testImplementation(libs.kotlinx.coroutines.test)
}
