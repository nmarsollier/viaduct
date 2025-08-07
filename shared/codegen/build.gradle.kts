plugins {
    id("kotlin-static-analysis")
}

tasks.test {
    jvmArgs = ((jvmArgs ?: emptyList<String>()) + "--add-opens=java.base/java.lang=ALL-UNNAMED")
}

dependencies {
    implementation(project(":shared:invariants"))
    implementation(project(":shared:utils"))
    implementation(libs.antlr.st4)
    implementation(libs.javassist)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.metadata.jvm)

    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotlin.reflect)
}
