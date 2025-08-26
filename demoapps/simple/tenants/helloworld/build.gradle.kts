plugins {
    `java-library`
    id("viaduct-tenant")
    kotlin("jvm")
}

viaductTenant {
    create("helloworld") {
        packageName.set("com.example.viadapp")
        schemaDirectory("${project.rootDir}/tenants/helloworld/src/main/resources/schema")
        schemaProjectPath.set(":schema")
        schemaName.set("schema")
    }
}

dependencies{
    implementation("com.airbnb.viaduct:runtime:0.1.0")
    implementation("ch.qos.logback:logback-classic:1.3.7")
}
