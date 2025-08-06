plugins {
    id("viaduct-tenant")
}

viaductTenant {
    create("tenant1") {
        packageName.set("viaduct.testapps.scopes")
        schemaDirectory("${project.rootDir}/tenant/testapps/scopes/tenants/tenant1/src/main/resources/viaduct/testapps/scopes/tenant1")
        schemaProjectPath.set(":tenant:testapps:scopes")
        schemaName.set("scopes")
    }
}

dependencies {
    implementation(project(":tenant:tenant-api"))
    implementation(project(":tenant:tenant-runtime"))
    implementation(libs.graphql.java)
}
