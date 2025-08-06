dependencies {
    implementation(project(":engine:engine-api"))
    implementation(project(":engine:engine-runtime"))
    implementation(project(":shared:graphql"))
    implementation(project(":shared:logging"))
    implementation(project(":shared:utils"))
    implementation(project(":tenant:tenant-api"))
    implementation(project(":tenant:tenant-runtime"))
    implementation(libs.graphql.java)
    implementation(libs.guice)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
