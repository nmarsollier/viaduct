package viaduct.api.internal

import viaduct.utils.api.InternalApi

/** Used to annotate generated resolver base classes */
@Target(AnnotationTarget.CLASS)
@InternalApi
annotation class ResolverFor(
    val typeName: String,
    val fieldName: String,
)
