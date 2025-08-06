plugins {
    id("viaduct-app")
}

dependencies {
    implementation(project(":service:service-api"))
    implementation(project(":service:service-runtime"))
    implementation(project(":tenant:tenant-runtime"))
    implementation(project(":tenant:testapps:schemaregistration:schema"))
    implementation(project(":tenant:testapps:schemaregistration:tenants:tenant1"))
    implementation(project(":tenant:testapps:schemaregistration:tenants:tenant2"))
    implementation(libs.graphql.java)

    testImplementation(testFixtures(project(":shared:graphql")))
    testImplementation(project(":tenant:testapps:fixtures"))
}
