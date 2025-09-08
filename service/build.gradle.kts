plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.dokka")
    id("dokka")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:2.0.0")
    }
}

dependencies {
    dokka(project(":service:service-wiring"))
    dokka(project(":service:service-api"))
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("docs/static/apis/service"))
        includes.from(layout.projectDirectory.file("module.md"))
    }
    pluginsConfiguration.html {
        customStyleSheets.from(rootProject.file("docs/kdoc-service-styles.css"))
    }
}
