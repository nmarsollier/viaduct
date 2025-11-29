package viaduct.utils.api

import kotlin.RequiresOptIn.Level
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.TYPEALIAS

/**
 * Marks an API as internal, i.e., it is not intended for use outside of Viaduct.
 * These classes can be changed or removed.
 */
@Retention(BINARY)
@RequiresOptIn(
    message = "This API is internal, is not meant to be used outside AirBnb.",
    level = Level.WARNING,
)
@Target(
    CLASS,
    FUNCTION,
    PROPERTY,
    CONSTRUCTOR,
    TYPEALIAS,
)
annotation class InternalApi
