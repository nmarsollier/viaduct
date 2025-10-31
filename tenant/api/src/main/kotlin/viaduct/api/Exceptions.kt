package viaduct.api

import java.lang.reflect.InvocationTargetException

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

/**
 * Used to wrap non-framework exceptions that are thrown while executing tenant resolver code.
 * This is tied to a specific tenant-written resolver.
 */
class ViaductTenantResolverException constructor(
    override val cause: Throwable,
    val resolver: String
) : Exception(cause), ViaductTenantException {
    // The call chain of resolvers, e.g. "User.fullName > User.firstName" means
    // User.fullName's resolver called User.firstName's resolver which threw an exception
    val resolversCallChain: String by lazy {
        generateSequence(this) { it.cause as? ViaductTenantResolverException }
            .map { it.resolver }
            .joinToString(" > ")
    }
}

/**
 * Catches any exception thrown by [resolveFn] (which must be called via reflection) and wraps it
 * in [ViaductTenantResolverException] unless it's a [ViaductFrameworkException].
 */
suspend fun wrapResolveException(
    resolverId: String,
    resolveFn: suspend () -> Any?
): Any? {
    return try {
        resolveFn()
    } catch (e: Exception) {
        // Since the resolver function is called via reflection, exceptions thrown from inside
        // the resolver may be wrapped in an InvocationTargetException.
        val resolverException = if (e is InvocationTargetException) {
            e.targetException
        } else {
            e
        }
        if (resolverException is ViaductFrameworkException) throw resolverException
        throw ViaductTenantResolverException(resolverException, resolverId)
    }
}

object ExceptionsForTesting {
    private class TestViaductTenantException(m: String) : ViaductTenantException, Exception(m)

    fun throwViaductFrameworkException(m: String): Nothing = throw ViaductFrameworkException(m)

    fun throwViaductTenantException(m: String): Nothing = throw TestViaductTenantException(m)
}
