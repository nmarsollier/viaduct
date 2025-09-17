plugins {
    `kotlin-dsl`
    id("build-logic-utils")
}

description = "Provides SETTINGS level convention plugins for the build"

dependencies {
    // Our internal gradle enterprise deployment can't handle a higher version
    implementation(plugin(libs.plugins.gradle.enterprise))
    implementation(plugin(libs.plugins.foojay.resolver.convention))
    implementation(plugin(libs.plugins.dokka))
}