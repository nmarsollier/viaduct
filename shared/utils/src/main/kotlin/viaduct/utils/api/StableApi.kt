package viaduct.utils.api

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

/**
 * Marks an API as stable, i.e., it will be maintained with backward compatibility
 * guarantees for a reasonable amount of time.
 *
 * This annotation can be applied to classes, functions, and properties to indicate
 * that they are part of the stable API surface.
 *
 * Binary compatibility is checked for release purposes for classes annotated with [viaduct.utils.api.StableApi].
 */
@Target(
    CLASS,
    FUNCTION,
    PROPERTY,
    PROPERTY_GETTER,
    PROPERTY_SETTER
)
@Retention(AnnotationRetention.BINARY)
annotation class StableApi
