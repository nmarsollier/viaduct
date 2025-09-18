plugins {
    `kotlin-dsl`
    id("build-logic-utils")
}

description = "Provides PROJECT level convention plugins for the build"

dependencies {
    implementation(plugin(libs.plugins.kotlin.jvm))
    implementation(plugin(libs.plugins.gradle.maven.publish))

    implementation(plugin(libs.plugins.detekt))
    implementation(plugin(libs.plugins.ktlintPlugin))

    implementation(plugin(libs.plugins.dokka))

    implementation(plugin(libs.plugins.dependency.analysis))
}
