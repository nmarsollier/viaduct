plugins {
    id("conventions.kotlin-project")
    id("conventions.kotlin-static-analysis")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
