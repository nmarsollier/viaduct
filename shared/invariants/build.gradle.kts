plugins {
    id("kotlin-project")
    id("kotlin-static-analysis")
}

dependencies {
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotest.assertions.shared)
}
