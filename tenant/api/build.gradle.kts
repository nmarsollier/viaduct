plugins {
    `java-library`
    id("kotlin-project")
    `maven-publish`
    id("test-feature-app")
    `java-test-fixtures`
    id("kotlin-static-analysis")
    id("dokka")
}

dependencies {
    api(libs.graphql.java)
    api(libs.guava)
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
    testImplementation(project(":tenant:tenant-runtime"))
    testImplementation(project(":shared:arbitrary"))
    testImplementation(project(":shared:graphql"))
    testImplementation(libs.io.mockk.dsl)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.kotest.property.jvm)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}
