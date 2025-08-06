plugins {
    id("viaduct-app")
}

dependencies {
    implementation(project(":tenant:tenant-runtime"))
    implementation(project(":tenant:testapps:hotloading:schema"))
    implementation(project(":tenant:testapps:hotloading:tenants:tenant1"))
    implementation(project(":tenant:testapps:hotloading:tenants:tenant2"))
    implementation(libs.graphql.java)

    testImplementation(testFixtures(project(":shared:graphql")))
    testImplementation(project(":tenant:testapps:fixtures"))
}
