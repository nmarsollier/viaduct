plugins {
    `java-library`
    id("com.airbnb.viaduct.module-gradle-plugin")
    kotlin("jvm")
}

viaductModule {
    modulePackageSuffix.set("helloworld")
}

dependencies{
    implementation("com.airbnb.viaduct:runtime:0.1.0")
    implementation("ch.qos.logback:logback-classic:1.3.7")
}
