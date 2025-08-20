plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
    id("kotlin-project")
    id("kotlin-static-analysis")
}

dependencies {
    api(libs.graphql.java)
    api(project(":engine:engine-api"))

    testImplementation(project(":service"))
    testImplementation(libs.kotest.property.jvm)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    testFixturesApi(project(":engine:engine-api"))
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
    repositories {
        mavenLocal()
    }
}
