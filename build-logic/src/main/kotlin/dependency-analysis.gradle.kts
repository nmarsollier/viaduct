plugins {
    id("com.autonomousapps.dependency-analysis")
}

configure<com.autonomousapps.DependencyAnalysisExtension> {
    issues {
        all {
            onAny {
                severity("fail")
            }
        }

        project(":tenant:tenant-api") {
            onAny { severity("warn") } // TODO: unused testImplementation("apischema") dependency introduced by viaduct-feature-app plugin
        }

        project(":tenant:tenant-codegen") {
            onAny { severity("warn") } // TODO: dependency on :tenant:tenant-api is somehow incorrectly identified, needs a better solution
        }

        project(":tenant:tenant-runtime") {
            onAny { severity("warn") } // TODO: bunch of unused testImplementation dependencies introduced by viaduct plugins
        }

        project(":shared:viaductschema") {
            onAny { severity("warn") } // TODO: complains about testImplementation(libs.junit) not being needed and I don't yet understand why
        }
    }

    structure {
        bundle("kotlin") {
            includeGroup("org.jetbrains.kotlin")
        }
    }
}