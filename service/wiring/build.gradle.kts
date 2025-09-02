plugins {
    id("kotlin-project")
}

dependencies {
    api(project(":service:service-api"))
    api(libs.graphql.java)

    implementation(project(":service:service-runtime"))
}
