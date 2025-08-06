plugins {
    `java-test-fixtures`
}

dependencies {
    api(libs.slf4j.api)

    implementation(project(":shared:utils"))
    implementation(libs.graphql.java)
    implementation(libs.graphql.java.extension)

    testFixturesImplementation(libs.graphql.java)
    testFixturesImplementation(libs.jackson.module)
    testFixturesImplementation(libs.kotlin.test)

    testImplementation(libs.guava)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.kotest.assertions.core.jvm)
}
