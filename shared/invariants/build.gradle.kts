plugins {
    id("kotlin-static-analysis")
}

dependencies {
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.kotest.assertions.core.jvm)
}
