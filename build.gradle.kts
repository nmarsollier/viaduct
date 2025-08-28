import org.gradle.internal.extensions.core.serviceOf
import org.gradle.api.tasks.SourceSetContainer

plugins {
    id("dependency-analysis")
    jacoco
}

val projectVersion = libs.versions.project
val groupId: String by project
group = groupId

val jarVersion = projectVersion.get()

subprojects {
    group = groupId
    version = jarVersion
}

tasks.register("cleanBuildAndPublish") {
    outputs.upToDateWhen { false }
    notCompatibleWithConfigurationCache("Uses exec and Gradle subprocesses")

    doLast {
        val execOperations = project.serviceOf<ExecOperations>()
        execOperations.exec { commandLine("./gradlew", ":runtime:publishToMavenLocal", "--no-configuration-cache", "--no-scan") }
    }
}

// Jacoco aggregate coverage report configuration
jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.register<JacocoReport>("jacocoAggregatedReport") {
    description = "Generates aggregated Jacoco coverage report for all subprojects"
    group = "verification"
    
    val javaSubprojects = subprojects.filter { it.plugins.hasPlugin("java") }
    
    dependsOn(subprojects.map { it.tasks.withType<Test>() })
    
    // Only depend on jacocoTestReport tasks that actually exist
    javaSubprojects.forEach { subproject ->
        subproject.plugins.withId("jacoco") {
            dependsOn(subproject.tasks.named("jacocoTestReport"))
        }
    }
    
    additionalSourceDirs.setFrom(javaSubprojects.map { it.extensions.getByName<SourceSetContainer>("sourceSets")["main"].allSource.srcDirs })
    sourceDirectories.setFrom(javaSubprojects.map { it.extensions.getByName<SourceSetContainer>("sourceSets")["main"].allSource.srcDirs })
    classDirectories.setFrom(javaSubprojects.map { it.extensions.getByName<SourceSetContainer>("sourceSets")["main"].output })
    executionData.setFrom(javaSubprojects.map { it.fileTree(it.layout.buildDirectory.asFile.get()).include("**/jacoco/*.exec") })
    
    reports {
        xml.required = true
        xml.outputLocation = layout.buildDirectory.file("reports/jacoco/aggregate/jacocoAggregatedReport.xml")
        html.required = true
        html.outputLocation = layout.buildDirectory.dir("reports/jacoco/aggregate/html")
        csv.required = false
    }
}

tasks.register<JacocoCoverageVerification>("jacocoAggregatedCoverageVerification") {
    description = "Verifies aggregated code coverage metrics"
    group = "verification"
    
    dependsOn(tasks.named("jacocoAggregatedReport"))
    
    val javaSubprojects = subprojects.filter { it.plugins.hasPlugin("java") }
    
    additionalSourceDirs.setFrom(javaSubprojects.map { it.extensions.getByName<SourceSetContainer>("sourceSets")["main"].allSource.srcDirs })
    sourceDirectories.setFrom(javaSubprojects.map { it.extensions.getByName<SourceSetContainer>("sourceSets")["main"].allSource.srcDirs })
    classDirectories.setFrom(javaSubprojects.map { it.extensions.getByName<SourceSetContainer>("sourceSets")["main"].output })
    executionData.setFrom(javaSubprojects.map { it.fileTree(it.layout.buildDirectory.asFile.get()).include("**/jacoco/*.exec") })
    
    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.00".toBigDecimal() // Start with 0% and gradually increase
            }
        }
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.00".toBigDecimal() // Start with 0% and gradually increase
            }
        }
    }
    
    doFirst {
        executionData = files(executionData.filter { it.exists() })
    }
}

// CircleCI-friendly task to run tests and generate coverage
tasks.register("testAndCoverage") {
    description = "Runs tests and generates coverage reports for CircleCI"
    group = "verification"
    
    dependsOn("jacocoAggregatedReport")
    
    doLast {
        println("Coverage reports generated:")
        println("- Individual module XML reports: */build/reports/jacoco/test/jacocoTestReport.xml")
        println("- Aggregated XML report: build/reports/jacoco/aggregate/jacocoAggregatedReport.xml")
        println("- Aggregated HTML report: build/reports/jacoco/aggregate/html/index.html")
    }
}
