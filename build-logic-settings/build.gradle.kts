plugins {
    `kotlin-dsl`
}

description = "Provides SETTINGS level convention plugins for the build"

dependencies {
    // Our internal gradle enterprise deployment can't handle a higher version
    implementation(plugin(libs.plugins.gradle.enterprise))
    implementation(plugin(libs.plugins.foojay.resolver.convention))
}

// Helper function that transforms a Gradle Plugin alias from a
// Version Catalog into a valid dependency notation for buildSrc
fun plugin(plugin: Provider<PluginDependency>) =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
