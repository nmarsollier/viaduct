package viaduct.gradle.settings

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

abstract class ViaductSettingsExtension(private val settings: Settings) {
    fun viaductInclude(pathOrNull: String? = null): Unit =
        settings.run {
            val path = pathOrNull ?: ""
            if (path.startsWith(":")) {
                throw GradleException("viaductInclude path should not start with ':'. Use '${path.substring(1)}' instead of '$path'")
            }

            val path2 = if (path == "") {
                ""
            } else {
                run {
                    include(path)
                    path + ":"
                }
            }
            include(path2 + "schema")

            // Include all build.gradle.kts-containing directories under "tenants"
            val startDir = rootDir.resolve((path2 + "tenants").replace(":", "/"))
            if (startDir.exists()) {
                startDir.walkTopDown().forEach {
                    if (it.isFile && it.name == "build.gradle.kts") {
                        val relativePath = it.parentFile.relativeTo(rootDir).invariantSeparatorsPath.replace("/", ":")
                        include(":$relativePath")
                    }
                }
            }
        }
}

class ViaductSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.extensions.create(
            "viaduct", // This will be visible as `viaduct.viaductInclude(...)`
            ViaductSettingsExtension::class.java,
            settings
        )
    }
}
