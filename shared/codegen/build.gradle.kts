plugins {
    id("kotlin-project")
    id("kotlin-static-analysis")
}

tasks.test {
    jvmArgs = ((jvmArgs ?: emptyList<String>()) + "--add-opens=java.base/java.lang=ALL-UNNAMED")
}

// Exclude from Dokka documentation due to parsing issues
tasks.named("dokkaHtmlPartial") {
    enabled = false
}

dependencies {
    api(libs.javassist)
    api(libs.kotlinx.metadata.jvm)
    api(project(":shared:invariants"))
    api(project(":shared:utils"))

    implementation(libs.antlr.st4)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotest.assertions.shared)
    testImplementation(libs.kotlin.reflect)
}
