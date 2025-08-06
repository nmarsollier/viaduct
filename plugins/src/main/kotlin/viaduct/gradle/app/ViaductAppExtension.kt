package viaduct.gradle.app

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

abstract class ViaductAppExtension
    @Inject
    constructor() {
        // Internal for now so can't (easily) be set via DLS, but keep door open to making it settable
        internal abstract val appProjectProperty: Property<Project>

        val appDir: Provider<Directory>
            get() = appProjectProperty.map { it.layout.projectDirectory }

        val appProject: Provider<Project>
            get() = appProjectProperty
    }
