package viaduct.tenant.runtime.execution

import java.lang.reflect.InvocationTargetException
import viaduct.api.ViaductFrameworkException
import viaduct.api.ViaductTenantException

/**
 * Used to wrap non-framework exceptions that are thrown while executing tenant resolver code.
 * This is tied to a specific tenant-written resolver.
 */
class ViaductTenantResolverException internal constructor(
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
