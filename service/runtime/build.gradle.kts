dependencies {
    implementation(project(":engine:engine-api"))
    implementation(project(":engine:engine-runtime"))
    implementation(project(":service:service-api"))
    implementation(project(":shared:graphql"))
    implementation(project(":shared:utils"))
    implementation(project(":tenant:tenant-api"))
    implementation(project(":tenant:tenant-runtime"))
    implementation(libs.caffeine)
    implementation(libs.classgraph)
    implementation(libs.graphql.java)
    implementation(libs.guice)
    implementation(libs.kotlinx.coroutines.core.jvm)
    implementation(libs.kotlinx.coroutines.jdk8)

    testImplementation(testFixtures(project(":engine:engine-api")))
    testImplementation(libs.io.mockk.jvm)
}
