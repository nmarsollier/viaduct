plugins {
    `kotlin-dsl`
}

description = "Provides convention plugins for the ROOT project"

dependencies {
    implementation(plugin(libs.plugins.dependency.analysis))
}

// Helper function that transforms a Gradle Plugin alias from a
// Version Catalog into a valid dependency notation for buildSrc
fun plugin(plugin: Provider<PluginDependency>) =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
