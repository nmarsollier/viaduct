plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
    id("kotlin-project")
    id("kotlin-static-analysis")
    id("dokka")
}

dependencies {
    api(libs.graphql.java)
    api(libs.viaduct.engine.api)

    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.kotest.property.jvm)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.strikt.core)

    testFixturesApi(libs.viaduct.engine.api)
}

mavenPublishing {
    pom {
        name.set("Viaduct [service-api]")
        description.set("The service API/SPI exposed by Viaduct.")
    }
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("docs/static/apis/"))
        includes.from(layout.projectDirectory.file("module.md"))
    }
    pluginsConfiguration.html {
        customStyleSheets.from(rootProject.file("docs/kdoc-service-styles.css"))
    }
}
