@file:Suppress("Detekt.TooGenericExceptionCaught")
@file:OptIn(ExperimentalCoroutinesApi::class)

package viaduct.deferred

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

fun <T> completedDeferred(value: T): Deferred<T> {
    val d = CompletableDeferred<T>()
    d.complete(value)
    return d
}

/** create a Deferred in an exceptional completed state */
fun <T> exceptionalDeferred(ex: Throwable): Deferred<T> {
    val d = CompletableDeferred<T>()
    d.completeExceptionally(ex)
    return d
}

fun <T : Any?> handle(
    block: () -> T,
    handler: (Any?, Throwable?) -> Any?
): Any? {
    @Suppress("UNCHECKED_CAST")
    return try {
        val result = block()
        if (result is Deferred<*>) {
            result as Deferred<T>
            result.handle(handler)
        } else {
            handler(result, null)
        }
    } catch (e: Throwable) {
        handler(null, e)
    }
}

fun <T> Result<T>.toDeferred(): Deferred<T> {
    val d = CompletableDeferred<T>()
    if (this.isSuccess) {
        d.complete(this.getOrThrow())
    } else {
        d.completeExceptionally(this.exceptionOrNull()!!)
    }
    return d
}

/**
 * Returns a new [Deferred] that, when this [Deferred] completes normally, is executed
 * with this [Deferred]'s result as the argument to [transform]. If this
 * [Deferred] completes exceptionally, the return value will also complete exceptionally
 * and [transform] will not be called.
 */
inline fun <T, R> Deferred<T>.thenApply(crossinline transform: (T) -> R): Deferred<R> {
    val d = CompletableDeferred<R>()
    this.invokeOnCompletion { throwable ->
        if (throwable != null) {
            d.completeExceptionally(throwable)
        } else {
            try {
                d.complete(transform(this.getCompleted()))
            } catch (ex: Throwable) {
                d.completeExceptionally(ex)
            }
        }
    }
    return d
}

fun <T : Any?, U : Any?> Deferred<T>.handle(handler: (T?, Throwable?) -> U): Deferred<U> {
    val d = CompletableDeferred<U>()
    this.invokeOnCompletion { throwable ->
        val transformed = if (throwable != null) {
            try {
                handler(null, throwable)
            } catch (e: Throwable) {
                d.completeExceptionally(e)
                return@invokeOnCompletion
            }
        } else {
            try {
                handler(this.getCompleted(), null)
            } catch (e: Throwable) {
                d.completeExceptionally(e)
                return@invokeOnCompletion
            }
        }
        d.complete(transformed)
    }
    return d
}

/**
 * Returns a new [Deferred] that, when this [Deferred] completes normally, is executed
 * with this [Deferred]'s result as the argument to [fn]. If this [Deferred] completes
 * exceptionally, the return value will also complete exceptionally and [transform]
 * will not be called.
 */
fun <T, R> Deferred<T>.thenCompose(fn: (T) -> Deferred<R>): Deferred<R> {
    val d = CompletableDeferred<R>()
    this.invokeOnCompletion { throwable ->
        if (throwable != null) {
            d.completeExceptionally(throwable)
        } else {
            try {
                val inner = fn(this.getCompleted())
                inner.invokeOnCompletion { innerThrowable ->
                    if (innerThrowable != null) {
                        d.completeExceptionally(innerThrowable)
                    } else {
                        d.complete(inner.getCompleted())
                    }
                }
            } catch (ex: Throwable) {
                d.completeExceptionally(ex)
            }
        }
    }
    return d
}

fun <T> Deferred<T>.exceptionally(fallback: (Throwable) -> T): Deferred<T> {
    val d = CompletableDeferred<T>()
    invokeOnCompletion { throwable ->
        if (throwable != null) {
            try {
                d.complete(fallback(throwable))
            } catch (ex: Throwable) {
                d.completeExceptionally(ex)
            }
        } else {
            d.complete(this.getCompleted())
        }
    }
    return d
}

inline fun <T> Deferred<T>.exceptionallyCompose(crossinline fallback: (Throwable) -> Deferred<T>): Deferred<T> {
    val d = CompletableDeferred<T>()
    this.invokeOnCompletion { throwable ->
        if (throwable != null) {
            try {
                val fallbackDeferred = fallback(throwable)
                fallbackDeferred.invokeOnCompletion { fallbackThrowable ->
                    if (fallbackThrowable != null) {
                        d.completeExceptionally(fallbackThrowable)
                    } else {
                        try {
                            d.complete(fallbackDeferred.getCompleted())
                        } catch (ex: Throwable) {
                            d.completeExceptionally(ex)
                        }
                    }
                }
            } catch (ex: Throwable) {
                d.completeExceptionally(ex)
            }
        } else {
            try {
                d.complete(this.getCompleted())
            } catch (ex: Throwable) {
                d.completeExceptionally(ex)
            }
        }
    }
    return d
}

/**
 * Composes two Deferred values by returning a new Deferred that:
 * 1) Awaits completion of this Deferred, then
 * 2) Invokes [combiner] with this Deferred's successful result and awaits [other],
 * 3) Completes with the result of [combiner].
 *
 * Because [other] is started independently, it may be running in parallel with this Deferred,
 * and might even finish first. This method will still yield the correct combined result, but
 * the callback for [other] is only attached *after* this Deferred completes. In other words,
 * we are chaining the completions rather than registering a single “two-input” callback up front.
 *
 * **Trade-offs and limitations**:
 * - We do not unify lifecycle or cancellation across the two Deferreds. If one fails or is canceled,
 *   that does not automatically stop the other. Exception propagation still works as expected for
 *   the final result, but we don’t provide sophisticated cancellation behavior.
 * - This approach is much simpler than a dedicated “both-input” concurrency construct (such as a
 *   `CompletableFuture`-style BiCompletion or Kotlin’s `awaitAll`), which can do things like
 *   cancel the other input on failure or attach callbacks to both simultaneously.
 *
 * A future implementation may address these tradeoffs, but for most straightforward needs—
 * where the two Deferreds are already running independently and you just want to combine their
 * eventual results—this is perfectly fine.
 */
fun <T, U, R> Deferred<T>.thenCombine(
    other: Deferred<U>,
    combiner: (T, U) -> R
): Deferred<R> =
    this.thenCompose { t ->
        other.thenApply { u -> combiner(t, u) }
    }
