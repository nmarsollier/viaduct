plugins {
    id("org.jetbrains.dokka")
    id("dokka")
}

dependencies {
    dokka(libs.viaduct.service.wiring)
    dokka(libs.viaduct.service.api)
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
