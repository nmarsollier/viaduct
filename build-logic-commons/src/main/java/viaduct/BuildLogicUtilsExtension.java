package viaduct;

import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.plugin.use.PluginDependency;

import org.gradle.api.provider.Provider;

public class BuildLogicUtilsExtension {

    /** Helper function that transforms a Gradle Plugin alias from a
     * Version Catalog into a valid dependency notation for buildSrc */
    public Provider<String> plugin(Provider<PluginDependency> pluginDependencyProvider) {
        return pluginDependencyProvider.map(pluginDependency -> {
            String pluginId = pluginDependency.getPluginId();
            VersionConstraint version = pluginDependency.getVersion();
            return pluginId + ":" + pluginId + ".gradle.plugin:" + version;
        });
    }

}
