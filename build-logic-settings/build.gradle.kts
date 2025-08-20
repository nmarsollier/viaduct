plugins {
    `kotlin-dsl`
}

description = "Provides SETTINGS level convention plugins for the build"

dependencies {
    // Our internal gradle enterprise deployment can't handle a higher version
    implementation("com.gradle.develocity:com.gradle.develocity.gradle.plugin:3.19.2")
}