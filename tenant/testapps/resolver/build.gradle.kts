plugins {
    id("viaduct-app")
}

dependencies {
    implementation(project(":tenant:tenant-runtime"))
    implementation(project(":tenant:testapps:resolver:schema"))
    implementation(project(":tenant:testapps:resolver:tenants:tenant1"))
    implementation(project(":tenant:testapps:resolver:tenants:tenant2"))
    implementation(project(":tenant:testapps:resolver:tenants:tenant3"))
    implementation(libs.graphql.java)

    testImplementation(testFixtures(project(":shared:graphql")))
    testImplementation(project(":tenant:testapps:fixtures"))
}
