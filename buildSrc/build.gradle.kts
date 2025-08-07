plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(plugin(libs.plugins.kotlin.jvm))

    implementation(plugin(libs.plugins.detekt))
    implementation(plugin(libs.plugins.ktlintPlugin))
}

// Helper function that transforms a Gradle Plugin alias from a
// Version Catalog into a valid dependency notation for buildSrc
fun DependencyHandlerScope.plugin(plugin: Provider<PluginDependency>) =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }