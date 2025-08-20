plugins {
    `java-test-fixtures`
    id("kotlin-project")
    id("kotlin-static-analysis")
}

dependencies {
    api(libs.javax.inject)
    api(libs.guice)
    api(project(":service:service-api"))

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.junit.params)
    testImplementation(libs.kotlinx.coroutines.debug)
    runtimeOnly(libs.kotlinx.coroutines.jdk8)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":shared:utils"))

    testCompileOnly(libs.kotlinx.coroutines.jdk8)

    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(project(":service:service-api"))
}
