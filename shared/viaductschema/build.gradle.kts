tasks.withType<Test>().configureEach {
    jvmArgs = listOf("-Xmx4g")
}

tasks.test {
    useJUnitPlatform()
    filter {
        includeTestsMatching("viaduct.graphql.schema.test.UtilsTest")
    }
    environment("PACKAGE_WITH_SCHEMA", "invalidschemapkg")
}

dependencies {
    implementation(project(":shared:invariants"))
    implementation(project(":shared:utils"))
    implementation(libs.graphql.java)
    implementation(libs.guava)
    implementation(libs.junit)
    implementation(libs.kotlin.reflect)
    implementation(libs.reflections)

    testImplementation(libs.guava.testlib)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.kotest.assertions.core.jvm)
}
