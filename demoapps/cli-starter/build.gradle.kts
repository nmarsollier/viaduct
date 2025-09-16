plugins {
    kotlin("jvm") version "1.9.24" // TODO - why doesn't it work with "1.8.22"
    alias(libs.plugins.viaduct.application)
    alias(libs.plugins.viaduct.module)
    application
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
