plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
}

project.version = libs.versions.project.get()

// These are the test plugins for internal Viaduct build usage
// (test-feature-app is not published: it's for internal testing purposes)
gradlePlugin {
    plugins {
        create("testSchema") {
            id = "test-schema"
            implementationClass = "viaduct.gradle.schema.ViaductSchemaPlugin"
        }
        create("testClassDiff") {
            id = "test-classdiff"
            implementationClass = "viaduct.gradle.classdiff.ViaductClassDiffPlugin"
        }
        create("testTenant") {
            id = "test-tenant"
            implementationClass = "viaduct.gradle.tenant.ViaductTenantPlugin"
        }
        create("testApp") {
            id = "test-app"
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
