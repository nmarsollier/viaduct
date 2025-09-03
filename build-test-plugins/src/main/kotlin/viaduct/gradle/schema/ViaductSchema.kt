package viaduct.gradle.schema

import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * This class represents a single schema that will be configured by name and will be generated on its own.
 * @param name the Name which the tasks will be created and the location of the generated classes will be based on.
 * @param project the implicit Gradle Project.
 */
open class ViaductSchema(val name: String, private val project: Project) {
    /**
     * This property holds the packageName where the generated Classes will be created
     */
    val grtPackageName: Property<String> = project.objects.property(String::class.java)

    /**
     * This function holds the target source set in which the generated Classes will be part of.
     */
    val targetSourceSets: SetProperty<String> = project.objects.setProperty(String::class.java)

    /**
     * The Schema files to be configured for generation
     */
    val schemaFiles: ConfigurableFileCollection = project.files()

    /**
     * The output directory for the generated classes that will be implemented in the used module
     */
    @Suppress("DEPRECATION")
    val generatedClassesDir: Property<File> = project.objects.property(File::class.java)
        .convention(File(project.buildDir, "generated-sources/schema/$name/generated_classes"))

    /**
     * Worker number needed by the Clikt command for generation
     */
    val workerNumber: Property<Int> = project.objects.property(Int::class.java).convention(0)

    /**
     * Worker count needed by the Clikt command for generation
     */
    val workerCount: Property<Int> = project.objects.property(Int::class.java).convention(1)

    /**
     * Flag to determine whether to include the schemas that are found for testing. Used by the Clikt Command.
     */
    val includeIneligibleForTesting: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(true)

    /**
     * Helper function to get the Schema resources
     */
    fun schemaDirectory(
        dir: String,
        pattern: String = "*.graphqls"
    ) {
        val fileTree = project.fileTree(dir) {
            include(pattern)
        }
        schemaFiles.from(fileTree)
    }
}
