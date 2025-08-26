plugins {
    id("kotlin-project-without-tests")
    id("viaduct-tenant")
}

viaductTenant {
    create("tenant1") {
        packageName.set("viaduct.testapps.schemaregistration")
        schemaDirectory(
            "${project.rootDir}/tenant/testapps/schemaregistration/tenants/tenant1/src/main/resources/viaduct/testapps/schemaregistration/tenant1"
        )
        schemaDirectory(
            "${project.rootDir}/tenant/testapps/schemaregistration/tenants/tenant2/src/main/resources/viaduct/testapps/schemaregistration/tenant2"
        )
        schemaProjectPath.set(":tenant:testapps:schemaregistration")
        schemaName.set("schemaregistration")
    }
}

dependencies {
    implementation(project(":tenant:tenant-api"))
    implementation(project(":tenant:tenant-runtime"))
    implementation(libs.graphql.java)
}
