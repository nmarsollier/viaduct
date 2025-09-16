plugins {
    id("com.autonomousapps.dependency-analysis")
}

configure<com.autonomousapps.DependencyAnalysisExtension> {
    issues {
        all {
            onAny {
                severity("warn")
            }
        }
    }

    structure {
        bundle("kotlin") {
            includeGroup("org.jetbrains.kotlin")
        }
    }
}