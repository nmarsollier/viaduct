plugins {
    `java-gradle-plugin`
}

description = "Provides convention plugins usable in any build logic anywhere, including other convention plugins"

gradlePlugin {
    plugins {
        create("build-logic-utils") {
            id = "build-logic-utils"
            implementationClass = "viaduct.BuildLogicUtilsPlugin"
        }
    }
}