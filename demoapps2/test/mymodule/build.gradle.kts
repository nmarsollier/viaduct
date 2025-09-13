plugins {
    kotlin("jvm") version "1.9.24" // TODO - why doesn't it work with "1.8.22"
    id("com.airbnb.viaduct.module-gradle-plugin")
}

viaductModule {
    modulePackageSuffix.set("helloworld")
}

dependencies {
    implementation("com.airbnb.viaduct:runtime:0.1.0")
}
