plugins {
    id("kotlin-project")
    id("test-app")
}

dependencies {
    implementation(project(":tenant:testapps:hotloading:schema"))

    testImplementation(libs.graphql.java)
    testImplementation(testFixtures(project(":shared:graphql")))
    testImplementation(project(":tenant:tenant-api"))
    testImplementation(project(":tenant:tenant-runtime"))
    testImplementation(project(":tenant:testapps:fixtures"))
    testImplementation(project(":tenant:testapps:hotloading:tenants:tenant1"))
    testImplementation(project(":tenant:testapps:hotloading:tenants:tenant2"))
}
