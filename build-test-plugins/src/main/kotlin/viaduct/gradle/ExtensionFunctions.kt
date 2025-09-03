package viaduct.gradle

import org.gradle.api.Project
import viaduct.gradle.app.ViaductAppExtension

val Project.viaduct: ViaductAppExtension
    get() = generateSequence(this) {
        it.parent
    }.firstNotNullOfOrNull {
        it.extensions.findByType(ViaductAppExtension::class.java)
    } ?: error("ViaductAppExtension not found in project hierarchy — did you apply the viaduct-app plugin?")
