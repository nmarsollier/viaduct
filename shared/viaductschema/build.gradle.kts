plugins {
    id("kotlin-project")
    id("kotlin-static-analysis")
}

tasks.test {
    filter {
        includeTestsMatching("viaduct.graphql.schema.test.UtilsTest")
    }
    environment("PACKAGE_WITH_SCHEMA", "invalidschemapkg")
}

dependencies {
    api(libs.graphql.java)
    api(libs.junit)
    api(project(":shared:invariants"))
    api(project(":shared:graphql"))
    api(project(":shared:utils"))

    implementation(libs.guava)
    implementation(libs.kotlin.reflect)
    implementation(libs.reflections)
    implementation(libs.jspecify)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.kotest.assertions.shared)
}
