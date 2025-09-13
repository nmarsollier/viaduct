plugins {
    kotlin("jvm") version "1.9.24"
    id("com.airbnb.viaduct.application-gradle-plugin")
    application
}

viaductApplication {
    grtPackageName.set("viaduct.api.grts")
    modulePackagePrefix.set("com.example.viadapp")
}

dependencies {
    runtimeOnly(project(":modules:helloworld"))

    implementation("ch.qos.logback:logback-classic:1.3.7")
    implementation("com.airbnb.viaduct:runtime:0.1.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.10")
}

application {
    mainClass.set("com.example.viadapp.ViaductApplicationKt")
}
