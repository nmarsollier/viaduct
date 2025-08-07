plugins {
    id("kotlin-static-analysis")
}

dependencies {
    implementation(libs.graphql.java)

    testImplementation(libs.io.mockk.jvm)
}
