plugins {
    `java-library`
    alias(libs.plugins.kotlinJvm)
    id("com.airbnb.viaduct.module-gradle-plugin")
}

viaductModule {
    modulePackageSuffix.set("tenant1")
}

dependencies {
    implementation(libs.viaduct.runtime)
    implementation(libs.spring.context)
}
