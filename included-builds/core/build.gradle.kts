plugins {
    id("viaduct-publishing") apply false
    id("orchestration")
    id("versioning")
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        pluginManager.apply("viaduct-publishing")
    }
}
