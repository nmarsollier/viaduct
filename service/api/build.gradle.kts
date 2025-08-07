plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
    id("kotlin-static-analysis")
}

dependencies {
    implementation(project(":engine:engine-api"))
    implementation(libs.graphql.java)

    testImplementation(project(":service"))
    testImplementation(libs.kotest.property.jvm)
    testImplementation(libs.kotlinx.coroutines.test)

    testFixturesImplementation(project(":engine:engine-api"))
    testFixturesImplementation(project(":service:service-bootapi"))
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
