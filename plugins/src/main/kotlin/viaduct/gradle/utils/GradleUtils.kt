package viaduct.gradle.utils

import java.io.File
import org.gradle.api.Project

/**
 * Function used to configure the kotlin source set. It adds the generatedSourceDir as part of the kotlin.
 * It will do nothing if kotlin is not set.
 *
 * @param project the project where its being used
 * @param generatedSourcesDir the File which contains the generated code
 */
fun configureKotlinSourceSet(
    project: Project,
    generatedSourcesDir: File
) {
    try {
        val kotlinExtension = project.extensions.findByName("kotlin") ?: return

        val sourceSetContainer = kotlinExtension.javaClass
            .getMethod("getSourceSets")
            .invoke(kotlinExtension)

        val testSourceSet = sourceSetContainer.javaClass
            .getMethod("getByName", String::class.java)
            .invoke(sourceSetContainer, "test")

        val kotlinSrcDir = testSourceSet.javaClass
            .getMethod("getKotlin")
            .invoke(testSourceSet)

        kotlinSrcDir.javaClass
            .getMethod("srcDir", Any::class.java)
            .invoke(kotlinSrcDir, generatedSourcesDir)
    } catch (e: Exception) {
        project.logger.debug("Kotlin plugin not found or failed to configure: ${e.message}")
    }
}
