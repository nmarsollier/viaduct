package viaduct.engine.runtime.execution

import java.util.concurrent.CompletableFuture
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.withContext

// This is a temporary fork of scopedFuture to be removed in the future.
// It is preserving the same signature without GlobalContext.
// Original implementation decorates context /w withGlobalContext() where as this one does not.
// https://sourcegraph.a.musta.ch/airbnb/treehouse/-/blob/common/kotlin/src/main/kotlin/com/airbnb/common/kotlin/coroutines/ThreadLocalCoroutineContextManager.kt?L152
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("TooGenericExceptionCaught")
fun <T> scopedFuture(block: suspend CoroutineScope.() -> T): CompletableFuture<T> = scopedAsync(block = block).asCompletableFuture()

/**
 * Starts a new coroutine within the [CoroutineScope] returned from [threadLocalCoroutineScope],
 * and returns its result as the value of a [Deferred].
 *
 * [scopedAsync] mimics the behavior of:
 *
 * ```
 * supervisorScope {
 *   async {
 *     ...
 *   }
 * }
 * ```
 *
 * but without being inside a `suspend` function.
 **/
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("TooGenericExceptionCaught")
fun <T> scopedAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    val threadLocalContext = threadLocalCoroutineContext()
    val parentJob = SupervisorJob(threadLocalContext[Job]!!)
    val deferred = CoroutineScope(threadLocalContext + parentJob).async(context, start, block)
    deferred.invokeOnCompletion { parentJob.complete() }
    return deferred
}

private fun threadLocalCoroutineContext(): CoroutineContext {
    val threadLocalContext = ThreadLocalCoroutineContextManager.INSTANCE.getCurrentCoroutineContext()
    val currentJob = threadLocalContext[Job]
    val contextForScope =
        if (currentJob?.isActive == true) {
            threadLocalContext
        } else {
            val rootJob = ThreadLocalCoroutineContextManager.INSTANCE.getCurrentDefaultJob()
            threadLocalContext + rootJob
        }
    return contextForScope
}

/**
 * Run the specified [block] with a [CoroutineContext] that contains a
 * [ThreadLocalCoroutineContextManager.ContextElement] [ContextElement].
 *
 * This function should be used to inject the proper [ContextElement] into the current
 * [CoroutineScope] in order to ensure proper proagation of the current coroutine context outside
 * of the coroutine hierarchy.
 *
 * In addition, we create a new [SupervisorJob], parented by the current [CoroutineContext]'s [Job],
 * and use that as the "default job" for the [ThreadLocalCoroutineContextManager.ContextElement]
 * [ContextElement]. This ensures that any "orphaned" coroutines launched within this context
 * still have a parent [Job] and maintains structured concurrency.
 */
@Suppress("TooGenericExceptionCaught")
suspend fun <T> CoroutineScope.withThreadLocalCoroutineContext(block: suspend CoroutineScope.() -> T): T {
    val job = SupervisorJob(this.coroutineContext[Job]!!)
    val contextElement = ThreadLocalCoroutineContextManager.ContextElement(job)
    try {
        return withContext(contextElement) { block() }
    } finally {
        job.complete()
    }
}

/**
 * [ThreadLocalCoroutineContextManager] manages a single [ThreadLocal] that, at any given time,
 * contains the currently active [coroutineContext]. This is used by [scopedFuture] (and its
 * helper functions) in order to allow launching [CompletableFuture]-based coroutines into a scope
 * that is properly parented by the [Job] that first launched the future.
 */
internal class ThreadLocalCoroutineContextManager {
    companion object {
        val INSTANCE = ThreadLocalCoroutineContextManager()
        private val threadLocal = ThreadLocal<CoroutineContext?>()
    }

    private var defaultCoroutineContext: CoroutineContext? = null

    /**
     * A [ThreadContextElement] that handles getting/setting the [ThreadLocal] managed by
     * [ThreadLocalCoroutineContextManager] as threads change during normal coroutine execution.
     */
    class ContextElement(val defaultJob: Job) :
        ThreadContextElement<CoroutineContext?>, AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<ContextElement>

        override fun updateThreadContext(context: CoroutineContext): CoroutineContext? {
            val old = threadLocal.get() // old coroutine context
            threadLocal.set(context)
            return old
        }

        override fun restoreThreadContext(
            context: CoroutineContext,
            oldState: CoroutineContext?
        ) {
            threadLocal.set(oldState)
        }
    }

    fun setDefaultCoroutineContext(coroutineContext: CoroutineContext?) {
        if (coroutineContext == null) {
            defaultCoroutineContext = null
            return
        }
        defaultCoroutineContext =
            // add a job if one does not exist
            if (coroutineContext[Job] == null) {
                coroutineContext + Job()
            } else {
                coroutineContext
            }
    }

    fun getCurrentCoroutineContext(): CoroutineContext =
        threadLocal.get() ?: defaultCoroutineContext ?: throw IllegalStateException(
            "Can't get CoroutineContext from the context manager, as no CoroutineContext is " +
                "active on this thread and no default CoroutineContext has been set."
        )

    fun getCurrentDefaultJob(): Job {
        val currentContext = getCurrentCoroutineContext()
        val contextElement = currentContext[ContextElement] as ContextElement
        return contextElement.defaultJob
    }
}
