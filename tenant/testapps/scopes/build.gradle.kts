plugins {
    id("viaduct-app")
}

dependencies {
    implementation(project(":tenant:tenant-api"))
    implementation(project(":tenant:tenant-runtime"))
    implementation(project(":tenant:testapps:scopes:tenants:tenant1"))
    implementation(libs.graphql.java)

    testImplementation(testFixtures(project(":shared:graphql")))
    testImplementation(project(":tenant:testapps:fixtures"))
}
