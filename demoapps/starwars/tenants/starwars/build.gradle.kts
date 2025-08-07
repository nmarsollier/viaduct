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
    create("starwars") {
        packageName.set("viaduct.demoapp")
        schemaDirectory("${project.viaduct.appDir.get()}/tenants/starwars/src/main/resources")
        // schemaProjectPath.set(":schema")
        schemaName.set("schema")
    }
}

dependencies {
    implementation(libs.viaduct.runtime)
    implementation(libs.spring.context)
}