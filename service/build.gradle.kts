plugins {
    id("kotlin-project-without-tests")
}

dependencies {
    implementation(libs.kotlin.reflect)

    testImplementation(libs.kotlin.test)
}
