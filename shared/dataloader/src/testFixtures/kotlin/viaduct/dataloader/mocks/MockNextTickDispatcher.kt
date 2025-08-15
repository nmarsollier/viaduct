package viaduct.dataloader.mocks

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.DelayController
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import viaduct.dataloader.NextTickDispatcher
import viaduct.service.api.spi.FlagManager

/**
 * This class is a combination of [TestCoroutineDispatcher] and [NextTickDispatcher]
 *
 * It is meant to be used in unit tests only.
 *
 * IMPORTANT: Do NOT use this in PROD code as the implementation
 * of [TestCoroutineDispatcher] is using a virtual clock (to speed up tests).
 */
@OptIn(InternalCoroutinesApi::class)
@ExperimentalCoroutinesApi
class MockNextTickDispatcher constructor(
    private val internalDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher(),
    private val batchQueueDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
) : NextTickDispatcher(internalDispatcher, batchQueueDispatcher, flagManager = FlagManager.disabled), Delay, DelayController {
    @ExperimentalCoroutinesApi
    override val currentTime: Long
        get() = internalDispatcher.currentTime

    @ExperimentalCoroutinesApi
    override fun advanceTimeBy(delayTimeMillis: Long): Long = internalDispatcher.advanceTimeBy(delayTimeMillis)

    @ExperimentalCoroutinesApi
    override fun advanceUntilIdle(): Long {
        var advanced: Long = 0L
        do {
            // advance our dispatchers in order. internalDispatcher will create nextTicks for batchQueueDispatcher
            // those nextTicks will add to the internalDispatcher until everything is executed
            advanced = Math.max(internalDispatcher.advanceUntilIdle(), batchQueueDispatcher.advanceUntilIdle())
        } while (advanced != 0L)
        return advanced
    }

    @ExperimentalCoroutinesApi
    override fun cleanupTestCoroutines() = internalDispatcher.cleanupTestCoroutines()

    @ExperimentalCoroutinesApi
    override fun pauseDispatcher() = internalDispatcher.pauseDispatcher()

    @ExperimentalCoroutinesApi
    override suspend fun pauseDispatcher(block: suspend () -> Unit) = internalDispatcher.pauseDispatcher(block)

    @ExperimentalCoroutinesApi
    override fun resumeDispatcher() = internalDispatcher.resumeDispatcher()

    @ExperimentalCoroutinesApi
    override fun runCurrent() = internalDispatcher.runCurrent()

    fun runBlockingTest(testBody: suspend TestCoroutineScope.() -> Unit) {
        kotlinx.coroutines.test.runBlockingTest(this, testBody)
    }

    @InternalCoroutinesApi
    override fun scheduleResumeAfterDelay(
        timeMillis: Long,
        continuation: CancellableContinuation<Unit>
    ) {
        internalDispatcher.scheduleResumeAfterDelay(timeMillis, continuation)
    }

    override fun invokeOnTimeout(
        timeMillis: Long,
        block: Runnable,
        context: CoroutineContext
    ): DisposableHandle {
        return internalDispatcher.invokeOnTimeout(timeMillis, block, context)
    }
}
