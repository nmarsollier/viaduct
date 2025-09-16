
plugins {
    id("kotlin-project")
    id("kotlin-static-analysis")
    id("viaduct-publishing")
}

dependencies {
    api(gradleApi())
}

viaductPublishing {
    artifactId.set("gradle-plugins-common")
}

mavenPublishing {
    pom {
        name.set("Viaduct [gradle-plugins-common]")
        description.set("Common libs used by Viaduct Gradle plugins.")
    }
}
