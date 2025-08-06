plugins {
    id("java")
    id("maven-publish")
}

val projectNames = setOf(
    ":engine:engine-api",
    ":engine:engine-runtime",
    ":service:service-api",
    ":service:service-bootapi",
    ":service:service-runtime",
    ":shared:arbitrary",
    ":shared:graphql",
    ":shared:logging",
    ":shared:utils",
    ":tenant:tenant-api",
    ":tenant:tenant-codegen",
    ":tenant:tenant-runtime",
    ":shared:arbitrary",
    ":shared:dataloader",
    ":shared:deferred",
    ":shared:graphql",
    ":shared:shared-codegen",
    ":shared:invariants",
    ":shared:logging",
    ":shared:viaductschema",
    ":snipped:errors",
)

val projectsToPackage = rootProject.subprojects.filter { it.path in projectNames }

projectsToPackage.forEach {
    evaluationDependsOn(it.path)
}

val tempDirectory = layout.buildDirectory.dir("merged-runtime")

val copyFiles = tasks.register<Copy>("copyFiles") {
    dependsOn(projectsToPackage.map { it.tasks.named("classes") })

    into(tempDirectory)

    projectsToPackage.forEach { sub ->
        val output = sub.extensions
            .getByType<SourceSetContainer>()["main"]
            .output

        from(output.classesDirs)
        from(output.resourcesDir)
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val combinedClasses = tasks.register<Jar>("combinedClasses") {
    dependsOn(copyFiles)
    archiveBaseName.set("runtime")
    from(tempDirectory)
}

val combinedSources = tasks.register<Jar>("combinedSources") {
    archiveClassifier.set("sources")
    from(
        projectsToPackage.mapNotNull {
            it.extensions.findByType<SourceSetContainer>()?.findByName("main")?.allSource
        }
    )
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    publications {
        create<MavenPublication>("runtimeCombined") {
            artifact(combinedClasses.get())
            artifact(combinedSources.get())
            artifactId = "runtime"
            version = project.version.toString()

            pom.withXml {
                val depsNode = asNode().appendNode("dependencies")
                projectsToPackage.forEach { sub ->
                    sub.configurations.findByName("implementation")?.allDependencies
                        ?.filterIsInstance<ExternalModuleDependency>()
                        ?.forEach { dep ->
                            val depNode = depsNode.appendNode("dependency")
                            depNode.appendNode("groupId", dep.group)
                            depNode.appendNode("artifactId", dep.name)
                            depNode.appendNode("version", dep.version)
                            depNode.appendNode("scope", "compile")
                        }
                }
            }
        }
    }

    repositories {
        mavenLocal()
    }
}

tasks.named("publishToMavenLocal") {
    dependsOn(combinedClasses)
}
