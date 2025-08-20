plugins {
    id("kotlin-project-without-tests")
}

dependencies {
    api(libs.graphql.java)
    api(libs.junit)
    api(project(":service:service-runtime"))
    api(project(":tenant:tenant-api"))
    api(project(":tenant:tenant-runtime"))

    implementation(testFixtures(project(":service:service-api")))
    implementation(project(":engine:engine-api"))
    implementation(project(":service:service-api"))
    implementation(project(":snipped:errors"))
    implementation(libs.guice)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core.jvm)
    implementation(libs.kotlinx.coroutines.jdk8)
}
