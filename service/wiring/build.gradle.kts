plugins {
    id("kotlin-project")
    id("dokka")
}

dependencies {
    api(project(":service:service-api"))
    api(libs.graphql.java)

    implementation(project(":service:service-runtime"))
    implementation(project(":tenant:tenant-runtime"))
    testImplementation(testFixtures(project(":engine:engine-api")))
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
