import org.gradle.internal.extensions.core.serviceOf
import org.gradle.api.tasks.SourceSetContainer

plugins {
    id("dependency-analysis")
    jacoco
    `jacoco-report-aggregation`
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
        execOperations.exec { commandLine("./gradlew", "clean") }
        execOperations.exec { commandLine("./gradlew", ":plugins:publishToMavenLocal", "--no-configuration-cache") }
        execOperations.exec { commandLine("./gradlew", ":runtime:publishToMavenLocal", "--no-configuration-cache", "--no-scan") }
    }
}

// Jacoco configuration
jacoco {
    toolVersion = libs.versions.jacoco.get()
}

// Dependencies for jacoco aggregation - all Java subprojects with jacoco
dependencies {
    jacocoAggregation(project(":engine:engine-api"))
    jacocoAggregation(project(":engine:engine-runtime"))
    jacocoAggregation(project(":runtime"))
    jacocoAggregation(project(":service:service-api"))
    jacocoAggregation(project(":service:service-runtime"))
    jacocoAggregation(project(":shared:arbitrary"))
    jacocoAggregation(project(":shared:dataloader"))
    jacocoAggregation(project(":shared:deferred"))
    jacocoAggregation(project(":shared:graphql"))
    jacocoAggregation(project(":shared:invariants"))
    jacocoAggregation(project(":shared:logging"))
    jacocoAggregation(project(":shared:shared-codegen"))
    jacocoAggregation(project(":shared:utils"))
    jacocoAggregation(project(":shared:viaductschema"))
    jacocoAggregation(project(":snipped:errors"))
    jacocoAggregation(project(":tenant:tenant-api"))
    jacocoAggregation(project(":tenant:tenant-codegen"))
    jacocoAggregation(project(":tenant:tenant-runtime"))
    jacocoAggregation(project(":tenant:testapps:policycheck"))
    jacocoAggregation(project(":tenant:testapps:resolver"))
    jacocoAggregation(project(":tenant:testapps:schemaregistration"))
}

// Configure the coverage report in the reporting block
reporting {
    reports {
        val testCodeCoverageReport by creating(JacocoCoverageReport::class) {
            testSuiteName = "test"
        }
    }
}

// Coverage verification with reasonable thresholds
tasks.register<JacocoCoverageVerification>("testCodeCoverageVerification") {
    dependsOn("testCodeCoverageReport")
    
    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.10".toBigDecimal() // 10% minimum instruction coverage
            }
        }
        rule {
            limit {
                counter = "BRANCH"  
                value = "COVEREDRATIO"
                minimum = "0.05".toBigDecimal() // 5% minimum branch coverage
            }
        }
    }
}

// CircleCI-friendly task to run tests and generate coverage
tasks.register("testAndCoverage") {
    description = "Runs tests and generates coverage reports for CircleCI"
    group = "verification"
    
    dependsOn("testCodeCoverageReport")
    
    doLast {
        println("Coverage reports generated:")
        println("- Individual module XML reports: */build/reports/jacoco/test/jacocoTestReport.xml")
        println("- Aggregated XML report: build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml")
        println("- Aggregated HTML report: build/reports/jacoco/testCodeCoverageReport/html/index.html")
    }
}

