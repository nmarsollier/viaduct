plugins {
    id("kotlin-project")
    id("test-app")
}

dependencies {
    implementation(project(":tenant:testapps:policycheck:schema"))

    testImplementation(libs.graphql.java)
    testImplementation(testFixtures(project(":shared:graphql")))
    testImplementation(project(":tenant:tenant-runtime"))
    testImplementation(project(":tenant:testapps:fixtures"))
    testImplementation(project(":tenant:testapps:policycheck:tenants:tenant1"))
}
