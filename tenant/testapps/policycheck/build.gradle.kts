plugins {
    id("viaduct-app")
}

dependencies {
    implementation(project(":snipped:errors"))
    implementation(project(":tenant:tenant-runtime"))
    implementation(project(":tenant:testapps:policycheck:schema"))
    implementation(project(":tenant:testapps:policycheck:tenants:tenant1"))
    implementation(libs.graphql.java)

    testImplementation(testFixtures(project(":shared:graphql")))
    testImplementation(project(":tenant:testapps:fixtures"))
}
