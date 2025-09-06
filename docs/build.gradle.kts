import java.net.URI
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.dokka")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:2.0.0")
    }
}

dependencies {
    dokka(project(":engine:engine-api"))
    dokka(project(":tenant:tenant-api"))
    dokka(project(":service:service-api"))
}

dokka {
    moduleName.set("Viaduct")
    moduleVersion.set(project.version.toString())

    pluginsConfiguration.html {
        homepageLink = "https://viaduct.airbnb.tech"
        customAssets.from("assets/icons/logo.svg")
        footerMessage = "&copy; 2025 Airbnb, Inc."
    }

    dokkaPublications.html {
        failOnWarning.set(true)
        suppressObviousFunctions.set(true)
        suppressInheritedMembers.set(true)
        outputDirectory.set(layout.projectDirectory.dir("static/api"))
    }

    dokkaSourceSets.configureEach {
        documentedVisibilities.set(
            setOf(
                VisibilityModifier.Public,
                VisibilityModifier.Protected,
            )
        )

        sourceLink {
            localDirectory.set(rootProject.projectDir)
            remoteUrl.set(
                URI(
                    "https://github.com/airbnb/viaduct/tree/master/"
                )
            )
            remoteLineSuffix.set("#L")
        }
    }
}
