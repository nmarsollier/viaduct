plugins {
    id("kotlin-static-analysis")
    id("me.champeau.jmh").version("0.7.3")
}

dependencies {
    api(libs.slf4j.api)

    implementation(libs.graphql.java)
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.guava.testlib)
    testImplementation(libs.io.mockk.jvm)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.konsist)

    jmh(libs.jmh.annotation.processor)
    jmh(libs.jmh.core)
}
