package viaduct.api.exception

/**
 * Exception thrown when a value cannot be mapped to the desired type.
 *
 * @param message the detail message for this exception
 */
@Suppress("unused")
class ValueMapperException(message: String, cause: Throwable? = null) : Exception(message, cause)
