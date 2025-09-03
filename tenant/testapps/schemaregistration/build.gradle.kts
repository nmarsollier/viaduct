plugins {
    id("kotlin-project")
    id("test-app")
}

dependencies {
    implementation(project(":tenant:testapps:schemaregistration:schema"))

    testImplementation(libs.graphql.java)
    testImplementation(testFixtures(project(":shared:graphql")))
    testImplementation(project(":engine:engine-api"))
    testImplementation(project(":service:service-runtime"))
    testImplementation(project(":tenant:tenant-api"))
    testImplementation(project(":tenant:tenant-runtime"))
    testImplementation(project(":tenant:testapps:fixtures"))
    testImplementation(project(":tenant:testapps:schemaregistration:tenants:tenant1"))
    testImplementation(project(":tenant:testapps:schemaregistration:tenants:tenant2"))
}
