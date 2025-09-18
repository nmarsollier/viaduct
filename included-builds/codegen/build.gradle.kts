plugins {
    id("conventions.viaduct-publishing") apply false
    id("orchestration")
    id("versioning")
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        pluginManager.apply("conventions.viaduct-publishing")
    }
}
