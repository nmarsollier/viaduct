import buildlogic.repoRoot

plugins {
    id("kotlin-project")
    id("dokka")
}

viaductPublishing {
    name.set("Viaduct Service Wiring")
    description.set("Bindings between the tenant and engine runtimes.")
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
        outputDirectory.set(repoRoot().dir("docs/static/apis/service"))
        includes.from(layout.projectDirectory.file("module.md"))
    }
    pluginsConfiguration.html {
        customStyleSheets.from(repoRoot().file("docs/kdoc-service-styles.css"))
    }
}
