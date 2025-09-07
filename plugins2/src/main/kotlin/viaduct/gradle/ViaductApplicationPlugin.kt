package viaduct.gradle

import java.io.File
import kotlin.io.path.writeText
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.jvm.tasks.ProcessResources
import viaduct.tenant.codegen.cli.SchemaObjectsBytecode

open class ViaductApplicationExtension(objects: org.gradle.api.model.ObjectFactory) {
    /** Kotlin package name for generated GRT classes. */
    val grtPackageName = objects.property(String::class.java).convention("viaduct.api.grts")
    
    /** Kotlin package name prefix for all modules. */
    val modulePackagePrefix = objects.property(String::class.java)
}

class ViaductApplicationPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = with(project) {
        require(this == rootProject) {
            // TODO -- relax this restriction someday, want to work well in a monorepo
            "Apply 'viaduct-application' only to the root project."
        }

        val appExt = extensions.create("viaductApplication", ViaductApplicationExtension::class.java, objects)
        val centralSchemaDir: Provider<Directory> = layout.buildDirectory.dir("viaduct/centralSchema")

        val generateCentralSchemaTask = generateCentralSchemaTask(centralSchemaDir)
        generateGRTsTask(
            appExt,
            centralSchemaDir,
            generateCentralSchemaTask,
        )
        
        plugins.withId("java") {
            tasks.named<ProcessResources>("processResources").configure { // TODO - better to do in Jar task??
                dependsOn(generateCentralSchemaTask) // TODO - is there a better way?
                from(centralSchemaDir) {
                    into("viaduct/centralSchema")
                    exclude("**/$BASE_SCHEMA_FILE") // TODO -- depends on skevy's work
                    includeEmptyDirs = false
                }
            }
        }
    }

    /** Synchronize all modules schema partition's into a single directory. */
    private fun Project.generateCentralSchemaTask(centralSchemaDir: Provider<Directory>): TaskProvider<*> {
        // Resolvable config that will collect all modules’ schema partitions
        val allPartitions = configurations.create(ViaductPluginCommon.Configs.ALL_SCHEMA_PARTITIONS_INCOMING).apply {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes { attribute(ViaductPluginCommon.VIADUCT_KIND, ViaductPluginCommon.Kind.SCHEMA_PARTITION) }
        }
        val artifacts = allPartitions.incoming.artifactView({}).files

        val generateCentralSchemaTask = tasks.register<Sync>("generateViaductCentralSchema") {
            group = "viaduct"
            description = "Collect schema files from all modules into a single directory."

            into(centralSchemaDir)
            from(allPartitions.incoming.artifactView {}.files) {
                include("**/*.graphqls")
            }

            doLast {
                val baseFile = centralSchemaDir.get().asFile.resolve(BASE_SCHEMA_FILE)
                baseFile.writeText(BASE_SCHEMA_CONTENT)
            }
        }

        // Publish the central schema as a consumable artifact
        // Intended for viaduct-module projects and other projects to cleanly consumer the central schema
        // Since the generateGRTs task is internal to the generateCentralSchema project, it uses centralSchemaDir directly
        configurations.create(ViaductPluginCommon.Configs.CENTRAL_SCHEMA_OUTGOING).apply {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes { attribute(ViaductPluginCommon.VIADUCT_KIND, ViaductPluginCommon.Kind.CENTRAL_SCHEMA) }
            outgoing.artifact(centralSchemaDir) { builtBy(generateCentralSchemaTask) }
        }

        return generateCentralSchemaTask
    }

    /** Call the bytecode-generator to generate GRT files. */
    private fun Project.generateGRTsTask(
        appExt: ViaductApplicationExtension,
        centralSchemaDir: Provider<Directory>,
        generateCentralSchemaTask: TaskProvider<*>,
    ) : TaskProvider<*> {
        // Build a FileCollection from the plugin's classloader URLs (includes plugin impl deps like :tenant:codegen)
        val pluginClasspath = files(ViaductPluginCommon.getClassPathElements(this@ViaductApplicationPlugin::class.java))

        val grtClassesDir: Provider<Directory> = layout.buildDirectory.dir("viaduct/grtClasses")

        val generateGRTClassesTask = tasks.register<JavaExec>("generateViaductGRTClassFiles") {
            // Make sure central schema exists first
            dependsOn(generateCentralSchemaTask)

            inputs.dir(centralSchemaDir).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("viaductCentralSchemaDir")
            outputs.dir(grtClassesDir).withPropertyName("viaductGRTClassesDir")

            // Use the plugin's classpath (contains :tenant:codegen and its deps)
            classpath = pluginClasspath
            mainClass.set(CODEGEN_MAIN_CLASS)

            doFirst {
                val csFiles = project.fileTree(centralSchemaDir).files
                val centralSchemaFilePaths = csFiles.map { it.absolutePath }.sorted().joinToString(",")
                val pkg = appExt.grtPackageName.get()
                val grtClassesDirPath = grtClassesDir.get().asFile.apply { mkdirs() }.absolutePath

                args(
                    "--schema_files", centralSchemaFilePaths,
                    "--pkg_for_generated_classes", pkg,
                    "--generated_directory", grtClassesDirPath
                )
            }
        }

        val generateGRTsTask = tasks.register<Jar>("generateViaductGRTs") {
            group = "viaduct"
            description = "Generate compiled GRT class files from the central schema."

            from(grtClassesDir)
            archiveBaseName.set("viaduct-grt")
            includeEmptyDirs = false
            dependsOn(generateGRTClassesTask)
        }

        // Publish the generated GRT classes as a consumable artifact
        configurations.create(ViaductPluginCommon.Configs.GRT_CLASSES_OUTGOING).apply {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                attribute(ViaductPluginCommon.VIADUCT_KIND, ViaductPluginCommon.Kind.GRT_CLASSES)

                // These will make us more friendly to IDEs and other tools
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements::class.java, LibraryElements.JAR))
            }
            outgoing.artifact(generateGRTsTask.flatMap { it.archiveFile })
        }

        return generateGRTsTask
    }

    companion object {
        private val CODEGEN_MAIN_CLASS = "viaduct.tenant.codegen.cli.SchemaObjectsBytecode\$Main"

        private val BASE_SCHEMA_FILE = "BASE_SCHEMA.graphqls"

        private val BASE_SCHEMA_CONTENT = """
           # --- Viaduct Base Schema (built in) ---
           # These schema elements are always built into Viaduct application schemas
           # --- End Viaduct Base Schema ---

           """.trimIndent()
    }
}
