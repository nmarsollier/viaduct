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

/*publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
    repositories {
        <define repository>
    }
}*/
// TODO: not necessary now, just for the demoapps;
//  might be needed later, when some version get released and
//  published to a real artefact repository
