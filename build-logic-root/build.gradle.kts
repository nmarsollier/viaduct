plugins {
    `kotlin-dsl`
    id("build-logic-utils")
}

description = "Provides convention plugins for the ROOT project"

dependencies {
    implementation(plugin(libs.plugins.dependency.analysis))
}