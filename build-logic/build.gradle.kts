plugins {
    `kotlin-dsl`
}

description = "Provides PROJECT level convention plugins for the build"

dependencies {
    implementation(plugin(libs.plugins.kotlin.jvm))

    implementation(plugin(libs.plugins.detekt))
    implementation(plugin(libs.plugins.ktlintPlugin))

    implementation(plugin(libs.plugins.dokka))
}

// Helper function that transforms a Gradle Plugin alias from a
// Version Catalog into a valid dependency notation for buildSrc
fun plugin(plugin: Provider<PluginDependency>) =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
