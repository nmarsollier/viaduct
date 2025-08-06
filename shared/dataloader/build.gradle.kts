plugins {
    `java-test-fixtures`
}

dependencies {
    implementation(libs.guice)
    implementation(libs.kotlinx.coroutines)
    implementation(project(":service:service-api"))

    testFixturesImplementation(libs.kotlinx.coroutines.jdk8)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(project(":service:service-api"))

    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.junit.params)
    testImplementation(libs.kotlinx.coroutines.debug)
    testImplementation(libs.kotlinx.coroutines.jdk8)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":shared:utils"))
}
