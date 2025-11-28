package viaduct.api.internal

import viaduct.utils.api.StableApi

/** Used to annotate generated resolver base classes */
@Target(AnnotationTarget.CLASS)
@StableApi
annotation class ResolverFor(
    val typeName: String,
    val fieldName: String,
)
