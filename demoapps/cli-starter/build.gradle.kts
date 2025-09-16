plugins {
    kotlin("jvm") version "1.9.10"
    id("com.airbnb.viaduct.application-gradle-plugin")
    id("com.airbnb.viaduct.module-gradle-plugin")
    application
}

kotlin {
    jvmToolchain(17)
}

viaductApplication {
    modulePackagePrefix.set("com.example.viadapp")
}

viaductModule {
    modulePackageSuffix.set("helloworld")
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("com.airbnb.viaduct:runtime:0.2.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.10")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.junit.platform:junit-platform-launcher:1.9.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")
}

application {
    mainClass.set("com.example.viadapp.ViaductApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}
