import viaduct.gradle.viaduct

plugins {
    `java-library`
    alias(libs.plugins.kotlinJvm)
    id("viaduct-tenant")
}

repositories {
    mavenCentral()
    mavenLocal()
}

viaductTenant {
    create("tenant1") {
        packageName.set("viaduct.demoapp")
        schemaDirectory("${project.viaduct.appDir.get()}/tenants/tenant1/src/main/resources/viaduct/demoapp/tenant1")
        schemaProjectPath.set(":schema")
        schemaName.set("schema")
    }
}

dependencies {
    implementation(libs.viaduct.runtime)
    implementation(libs.spring.context)
}
