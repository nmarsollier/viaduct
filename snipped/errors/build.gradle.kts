plugins {
    id("conventions.kotlin-project")
    id("conventions.kotlin-static-analysis")
}

dependencies {
    api(libs.graphql.java)

    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
}
