plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.dokka")
}

dependencies {
    dokka(libs.viaduct.tenant.api)
    dokka(libs.viaduct.service.api)
}
