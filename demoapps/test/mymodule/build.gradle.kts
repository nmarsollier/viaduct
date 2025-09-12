plugins {
    kotlin("jvm") version "1.9.10" // TODO - why doesn't it work with "1.8.22"
    id("viaduct-module")
}

viaductModule {
    modulePackageSuffix.set("helloworld")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.airbnb.viaduct:runtime:0.1.0")
}
