plugins {
    id("viaduct-publishing") apply false
    id("orchestration")
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        pluginManager.apply("viaduct-publishing")
    }
}
