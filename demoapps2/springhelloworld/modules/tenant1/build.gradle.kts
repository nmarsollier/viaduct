plugins {
    `java-library`
    alias(libs.plugins.kotlinJvm)
    id("viaduct-module")
}

viaductModule {
    modulePackageSuffix.set("tenant1")
}

dependencies {
    implementation(libs.viaduct.runtime)
    implementation(libs.spring.context)
}
