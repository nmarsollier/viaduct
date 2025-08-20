plugins {
    id("kotlin-project")
    id("kotlin-static-analysis")
}

dependencies {
    api(libs.graphql.java)

    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
}
