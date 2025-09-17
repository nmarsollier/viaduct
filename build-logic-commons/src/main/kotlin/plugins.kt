import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency

/**
 * Helper function that transforms a Gradle Plugin alias from a
 * Version Catalog into a valid dependency notation for buildSrc
 * */
fun plugin(plugin: Provider<PluginDependency>) =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }