plugins {
    id("kotlin-project")
    id("dokka")
}

dependencies {
    api(libs.viaduct.service.api)
    api(libs.graphql.java)

    implementation(libs.viaduct.service.runtime)
    implementation(libs.viaduct.tenant.runtime)
    testImplementation(testFixtures(libs.viaduct.engine.api))
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

mavenPublishing {
    pom {
        name.set("Viaduct [service-wiring]")
        description.set("Bindings between the tenant and engine runtimeBindings between the tenant and engine runtimess.")
    }
}
