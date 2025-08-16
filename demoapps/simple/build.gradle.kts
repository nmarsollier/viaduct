plugins {
    kotlin("jvm") version "1.9.24"
    id("viaduct-app") version "0.1.0-SNAPSHOT"
    application
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    runtimeOnly(project(":tenants:helloworld"))

    implementation("ch.qos.logback:logback-classic:1.3.7")
    implementation("com.airbnb.viaduct:runtime:0.1.0-SNAPSHOT")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.10")
    implementation(project(":schema"))
}

application {
    mainClass.set("com.example.viadapp.ViaductApplicationKt")
}
