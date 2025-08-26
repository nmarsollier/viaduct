plugins {
    id("kotlin-project-without-tests")
    id("viaduct-tenant")
}

viaductTenant {
    create("tenant1") {
        packageName.set("viaduct.testapps.policycheck")
        schemaDirectory("${project.rootDir}/tenant/testapps/policycheck/tenants/tenant1/src/main/resources/viaduct/testapps/policycheck/tenant1")
        schemaProjectPath.set(":tenant:testapps:policycheck")
        schemaName.set("policycheck")
    }
}

dependencies {
    implementation(project(":tenant:tenant-api"))
    implementation(project(":tenant:tenant-runtime"))
    implementation(libs.graphql.java)
}
