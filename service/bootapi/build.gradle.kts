plugins {
    id("kotlin-static-analysis")
}

dependencies {
    implementation(project(":engine:engine-api"))
    implementation(project(":service:service-api"))
    implementation(libs.graphql.java)
}
