plugins {
    `java-library`
    alias(libs.plugins.kotlinJvm)
    id("viaduct-module")
}

viaductModule {
    modulePackageSuffix.set("starwars")
}

dependencies {
    implementation(libs.viaduct.runtime)
    implementation(libs.spring.context)
}
