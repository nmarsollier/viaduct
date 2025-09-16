plugins {
    `kotlin-dsl`
    id("build-logic-utils")
}

description = "Provides PROJECT level convention plugins for the build"

dependencies {
    implementation(buildLogicUtils.plugin(libs.plugins.kotlin.jvm))

    implementation(buildLogicUtils.plugin(libs.plugins.detekt))
    implementation(buildLogicUtils.plugin(libs.plugins.ktlintPlugin))

    implementation(buildLogicUtils.plugin(libs.plugins.dokka))
}
