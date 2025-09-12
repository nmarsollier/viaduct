import java.net.URI
import kotlin.text.set
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    // Shared configuration for documented modules goes here
    moduleVersion.set(project.version.toString())

    pluginsConfiguration.html {
        homepageLink = "https://airbnb.io/viaduct"
        customStyleSheets.from(rootProject.file("docs/kdoc-styles.css"))
        customAssets.from(rootProject.file("docs/assets/icons/logo-only-white.svg"))
        footerMessage = "&copy; 2025 Airbnb, Inc."
    }

    dokkaPublications.html {
        failOnWarning.set(true)
        suppressObviousFunctions.set(true)
        suppressInheritedMembers.set(true)
        outputDirectory.set(rootProject.layout.projectDirectory.dir("docs/static/apis/" + project.name))
    }

    dokkaSourceSets.configureEach {
        if (layout.projectDirectory.file("module.md").asFile.exists()) {
            includes.from(layout.projectDirectory.file("module.md"))
        }

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
                    "https://github.com/airbnb/viaduct/tree/v" + project.version.toString()
                )
            )
            remoteLineSuffix.set("#L")
        }

        perPackageOption {
            matchingRegex.set(".*internal.*")
            suppress.set(true)
        }
    }
}

fun displayName(project: Project): String {
    return "Viaduct " + project.name
        .replace("api", "API")
        .replace("-", " ")
        .split(" ").joinToString(" ") { it.replaceFirstChar { it2 -> it2.uppercase() }  }
}
