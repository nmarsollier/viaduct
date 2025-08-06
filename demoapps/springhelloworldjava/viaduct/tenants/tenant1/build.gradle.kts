import viaduct.gradle.viaduct

plugins {
    `java-library`
    alias(libs.plugins.kotlinJvm)
    id("viaduct-tenant")
}

viaductTenant {
    create("tenant1") {
        packageName.set("viaduct.demoapp")
        schemaDirectory("${viaduct.appDir.get()}/tenants/tenant1/src/main/resources/viaduct/demoapp/tenant1")
        schemaName.set("schema")
    }
}

dependencies {
    implementation(libs.viaduct.runtime)
}
