package conventions

import kotlinx.validation.ApiValidationExtension

plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

configure<ApiValidationExtension> {
    nonPublicMarkers.add("viaduct.InternalApi")
    nonPublicMarkers.add("viaduct.TestingApi")
    nonPublicMarkers.add("viaduct.ExperimentalApi")
}