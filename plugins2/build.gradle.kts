plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
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
    implementation("com.airbnb.viaduct:tenant-codegen:${libs.versions.project.get()}:all") {
        isChanging = true
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
            artifactId = "plugins2"  // TODO - change to just "plugins" when ready
            version = project.version.toString()
        }
    }
}
