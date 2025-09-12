plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

group = "com.airbnb.viaduct" // TODO - don't hardwire this

project.version = libs.versions.project.get()

gradlePlugin {
    plugins {
        create("viaductApplication") {
            id = "viaduct-application"
            implementationClass = "viaduct.gradle.ViaductApplicationPlugin"
            displayName = "Viaduct Application Plugin"
            description = "Empty scaffold for the Viaduct application plugin."
            tags.set(listOf("viaduct", "graphql", "kotlin", "gradle"))
        }
        create("viaductModule") {
            id = "viaduct-module"
            implementationClass = "viaduct.gradle.ViaductModulePlugin"
            displayName = "Viaduct Module Plugin"
            description = "Empty scaffold for the Viaduct module plugin."
            tags.set(listOf("viaduct", "graphql", "kotlin", "gradle"))
        }
    }
}

dependencies {
    implementation("com.airbnb.viaduct:tenant-codegen:${libs.versions.project.get()}") {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements::class.java, LibraryElements.JAR))
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.SHADOWED))
        }
    }
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("viaductPluginLib") {
            from(components["java"])
            artifactId = "plugins2" // TODO - change to just "plugins" when ready
            version = project.version.toString()
        }
    }
}

detekt {
    source.setFrom("src/main/kotlin", "src/test/kotlin")
    config.setFrom("$projectDir/../detekt.yml")
    ignoreFailures = true
}

ktlint {
    version.set("1.2.1")
    enableExperimentalRules.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(true)

    filter {
        exclude { element ->
            element.file.path.contains("/generated-sources/")
        }
    }
}
