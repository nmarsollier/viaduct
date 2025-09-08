plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.dokka")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:2.0.0")
    }
}

dependencies {
    dokka(project(":tenant:tenant-api"))
    dokka(project(":service:service-api"))
}
