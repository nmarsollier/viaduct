plugins {
    `java-library`
    id("kotlin-project")
    `maven-publish`
    id("viaduct-feature-app")
    `java-test-fixtures`
    id("kotlin-static-analysis")
}

dependencies {
    api(libs.graphql.java)
    api(libs.javax.inject)
    api(project(":engine:engine-api"))

    implementation(project(":shared:utils"))
    implementation(project(":shared:viaductschema"))
    implementation(libs.kotlin.reflect)

    runtimeOnly(project(":tenant:tenant-codegen"))

    testFixturesApi(project(":engine:engine-api"))
    testFixturesApi(libs.graphql.java)
    testFixturesApi(project(":shared:viaductschema"))

    testFixturesImplementation(testFixtures(project(":engine:engine-api")))

    testImplementation(testFixtures(project(":engine:engine-api")))
    testImplementation(project(":shared:arbitrary"))
    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.kotest.property.jvm)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.strikt.core)
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

afterEvaluate {
    tasks.named("explodeCodeSourceTest") { // TODO: a hack for the sake of this dependency-analysis task...
        dependsOn(tasks.named("generateApischemaSchemaObjects"))
        dependsOn(tasks.named("generateApischemaTenant"))
    }
}
