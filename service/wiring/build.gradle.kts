plugins {
    id("kotlin-project")
    id("dokka")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:2.0.0")
    }
}

dependencies {
    api(project(":service:service-api"))
    api(libs.graphql.java)

    implementation(project(":service:service-runtime"))
    dokka(project(":service:service-api"))
    dokka(project(":service:service-wiring"))
}
