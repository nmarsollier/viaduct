plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm")
    id("viaduct-feature-app")
    `java-test-fixtures`
    id("kotlin-static-analysis")
}

dependencies {
    implementation(project(":engine:engine-api"))
    implementation(project(":shared:utils"))
    implementation(project(":shared:viaductschema"))
    implementation(libs.graphql.java)
    implementation(libs.guava)
    implementation(libs.javax.inject)
    implementation(libs.kotlin.reflect)

    testFixturesImplementation(testFixtures(project(":engine:engine-api")))
    testFixturesImplementation(testFixtures(project(":service:service-api")))
    testFixturesImplementation(project(":shared:viaductschema"))
    testFixturesImplementation(libs.graphql.java)

    testImplementation(testFixtures(project(":engine:engine-api")))
    testImplementation(project(":shared:arbitrary"))
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.kotest.property.jvm)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.strikt.core)
    testImplementation(libs.strikt.jvm)
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "api"
            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
    repositories {
        mavenLocal()
    }
}
