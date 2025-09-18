package viaduct.gradle.internal

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import java.io.File

/**
 * Best-effort repo root detector. Looks for any of the [sentinels] at or above this project.
 */
fun Project.repoRoot(vararg sentinels: String = arrayOf(".circleci", ".git")): DirectoryProperty {
    var cur: File? = rootDir
    while (cur != null) {
        if (sentinels.any { File(cur, it).exists() }) return objects.directoryProperty().fileValue(cur)
        cur = cur.parentFile
    }
    // Fallback: this build's root
    return objects.directoryProperty().fileValue(rootDir)
}