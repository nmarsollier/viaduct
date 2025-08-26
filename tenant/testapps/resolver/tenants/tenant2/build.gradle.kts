plugins {
    id("kotlin-project-without-tests")
    id("viaduct-tenant")
}

viaductTenant {
    create("tenant2") {
        packageName.set("viaduct.testapps.resolver")
        schemaDirectory("${project.rootDir}/tenant/testapps/resolver/tenants/tenant1/src/main/resources/viaduct/testapps/resolver/tenant1")
        schemaDirectory("${project.rootDir}/tenant/testapps/resolver/tenants/tenant2/src/main/resources/viaduct/testapps/resolver/tenant2")
        schemaDirectory("${project.rootDir}/tenant/testapps/resolver/tenants/tenant3/src/main/resources/viaduct/testapps/resolver/tenant3")
        schemaProjectPath.set(":tenant:testapps:resolver")
        schemaName.set("resolver")
    }
}

dependencies {
    implementation(project(":tenant:tenant-api"))
    implementation(project(":tenant:tenant-runtime"))
    implementation(libs.graphql.java)
}
