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
    create("starships") {
        packageName.set("viaduct.demoapp")
        schemaDirectory("${project.viaduct.appDir.get()}/tenants/starwars/src/main/resources")
        schemaDirectory("${project.viaduct.appDir.get()}/tenants/starships/src/main/resources")
        schemaProjectPath.set(":schema")
        schemaName.set("schema")
    }
}

dependencies {
    implementation(libs.viaduct.runtime)
    implementation(libs.spring.context)
    implementation(project(":schema"))
}

// Ensure starships tenant generation depends on schema generation
afterEvaluate {
    tasks.named("generateStarshipsTenant") {
        dependsOn(":schema:generateCombinedSchemaBytecode")
    }
}