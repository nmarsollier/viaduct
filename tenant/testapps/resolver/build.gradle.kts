plugins {
    id("kotlin-project")
    id("viaduct-app")
}

dependencies {
    implementation(project(":tenant:testapps:resolver:schema"))

    testImplementation(libs.graphql.java)
    testImplementation(project(":tenant:tenant-api"))
    testImplementation(project(":tenant:tenant-runtime"))
    testImplementation(project(":tenant:testapps:fixtures"))
    testImplementation(project(":tenant:testapps:resolver:tenants:tenant1"))
    testImplementation(project(":tenant:testapps:resolver:tenants:tenant2"))
    testImplementation(project(":tenant:testapps:resolver:tenants:tenant3"))
    testImplementation(testFixtures(project(":shared:graphql")))
}
