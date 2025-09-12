plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

project.version = libs.versions.project.get()

dependencies {
    implementation(libs.graphql.java)
}

// These are the plugins we're publishing externally for demoapp usage
gradlePlugin {
    plugins {
        create("viaductSchema") {
            id = "viaduct-schema"
            implementationClass = "viaduct.gradle.schema.ViaductSchemaPlugin"
        }
        create("viaductTenant") {
            id = "viaduct-tenant"
            implementationClass = "viaduct.gradle.tenant.ViaductTenantPlugin"
        }
        create("viaductApp") {
            id = "viaduct-app"
            implementationClass = "viaduct.gradle.app.ViaductAppPlugin"
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    publications {
        create<MavenPublication>("pluginJar") {
            from(components["java"])

            artifact(sourcesJar.get())

            artifactId = "plugins"
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
    version.set(libs.versions.ktlintVersion)
    enableExperimentalRules.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(true)

    filter {
        exclude { element ->
            element.file.path.contains("/generated-sources/")
        }
    }
}
