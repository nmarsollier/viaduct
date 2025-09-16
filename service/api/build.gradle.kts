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
    api(project(":engine:engine-api"))

    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.kotest.property.jvm)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.strikt.core)

    testFixturesApi(project(":engine:engine-api"))
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
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
