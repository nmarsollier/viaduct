
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
    name.set("Common Gradle Plugin Libraries")
    description.set("Common libs used by Viaduct Gradle plugins.")
}
