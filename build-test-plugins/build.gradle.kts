plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.viaduct.tenant.codegen)

    // Do NOT leak the Kotlin Gradle Plugin at runtime
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
}
