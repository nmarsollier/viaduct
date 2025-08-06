package viaduct.api

/**
 * Use this to wrap all entry points into the tenant API. This will catch any exception
 * and attribute it to the framework unless it's a ViaductTenantUsageException.
 */
internal fun <T> handleTenantAPIErrors(
    message: String,
    block: () -> T
): T {
    @Suppress("Detekt.TooGenericExceptionCaught")
    try {
        return block()
    } catch (e: Throwable) {
        if (e is ViaductTenantException) throw e
        throw ViaductFrameworkException("$message ($e)", e)
    }
}

/**
 * Same as handleTenantAPIErrors but for suspend functions
 */
internal suspend fun <T> handleTenantAPIErrorsSuspend(
    message: String,
    block: suspend () -> T
): T {
    @Suppress("Detekt.TooGenericExceptionCaught")
    try {
        return block()
    } catch (e: Throwable) {
        if (e is ViaductTenantException) throw e
        throw ViaductFrameworkException("$message ($e)", e)
    }
}

/**
 * Marker interface for exceptions that should be attributed to tenant code
 */
interface ViaductTenantException

/**
 * Used in the tenant API and dependencies to indicate that an error is due to framework code
 * and shouldn't be attributed to tenant code
 */
class ViaductFrameworkException internal constructor(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Used in framework code to indicate that an error is due to invalid usage of the tenant API
 * by tenant code.
 */
class ViaductTenantUsageException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause), ViaductTenantException
