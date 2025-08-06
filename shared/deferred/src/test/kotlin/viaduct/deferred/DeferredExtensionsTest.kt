@file:Suppress("ForbiddenImport")

package viaduct.deferred

import java.util.concurrent.CountDownLatch
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail

class DeferredExtensionsTest {
    @Nested
    inner class CompletedDeferredTests {
        @Test
        fun `completedDeferred returns a completed deferred with the correct value`() {
            runBlocking {
                val value = 42
                val deferred = completedDeferred(value)
                assertTrue(deferred.isCompleted)
                assertEquals(value, deferred.await())
            }
        }

        @Test
        fun `completedDeferred works with a null value`() {
            runBlocking {
                val deferred = completedDeferred<String?>(null)
                assertTrue(deferred.isCompleted)
                assertNull(deferred.await())
            }
        }
    }

    @Nested
    inner class ExceptionalDeferredTests {
        @Test
        fun `exceptionalDeferred returns a deferred completed exceptionally`() {
            runBlocking {
                val err = RuntimeException()
                val deferred = exceptionalDeferred<Int>(err)
                assertThrows<RuntimeException> { deferred.await() }
            }
        }
    }

    @Nested
    inner class HandleIfDeferredTests {
        @Test
        fun `handleIfDeferred calls handler for immediate value`() {
            val result = handle({ 5 }) { value, throwable ->
                if (throwable != null) "error" else value.toString()
            }
            assertEquals("5", result)
        }

        @Test
        fun `handleIfDeferred calls handler for exception thrown in block`() {
            val exception = IllegalArgumentException("oops")
            val result = handle<Int>({ throw exception }) { value, throwable ->
                throwable?.message ?: value.toString()
            }
            assertEquals("oops", result)
        }

        @Test
        fun `handleIfDeferred handles deferred returned from block`() {
            runBlocking {
                val deferred = completedDeferred(10)
                val result = handle({ deferred }) { value, throwable ->
                    if (throwable != null) "error" else (value as Int * 2).toString()
                }
                // When a block returns a Deferred, handleIfDeferred returns a Deferred.
                @Suppress("UNCHECKED_CAST")
                val finalResult = (result as Deferred<*>).await()
                assertEquals("20", finalResult)
            }
        }

        @Test
        fun `handleIfDeferred handles deferred exception`() {
            runBlocking {
                val exception = RuntimeException("fail")
                val failedDeferred = CompletableDeferred<Int>()
                failedDeferred.completeExceptionally(exception)
                val result = handle({ failedDeferred }) { _, throwable ->
                    throwable?.message ?: "no error"
                }

                @Suppress("UNCHECKED_CAST")
                val finalResult = (result as Deferred<*>).await()
                assertEquals("fail", finalResult)
            }
        }
    }

    @Nested
    inner class DeferredHandleIfDeferredExtensionTests {
        @Test
        fun `handleIfDeferred returns transformed result on normal completion`() {
            runBlocking {
                val deferred = completedDeferred(3)
                val transformed = deferred.handle { value, throwable ->
                    if (throwable != null) -1 else value!! * 2
                }
                assertEquals(6, transformed.await())
            }
        }

        @Test
        fun `handleIfDeferred returns fallback result on exception`() {
            runBlocking {
                val exception = IllegalStateException("error")
                val failedDeferred = CompletableDeferred<Int>()
                failedDeferred.completeExceptionally(exception)
                val handled = failedDeferred.handle { value, throwable ->
                    if (throwable != null) 0 else value!!
                }
                assertEquals(0, handled.await())
            }
        }
    }

    @Nested
    inner class ThenApplyTests {
        @Test
        fun `thenApply transforms completed deferred value`() {
            runBlocking {
                val deferred = completedDeferred(5)
                val transformed = deferred.thenApply { it * 3 }
                assertEquals(15, transformed.await())
            }
        }

        @Test
        fun `thenApply propagates exception from original deferred`() {
            runBlocking {
                val exception = RuntimeException("oops")
                val failedDeferred = CompletableDeferred<Int>()
                failedDeferred.completeExceptionally(exception)
                val transformed = failedDeferred.thenApply { it * 3 }
                val thrown = assertThrows<RuntimeException> { runBlocking { transformed.await() } }
                assertEquals("oops", thrown.message)
            }
        }

        @Test
        fun `thenApply propagates exception thrown in transform function`() {
            runBlocking {
                val deferred = completedDeferred(10)
                val exception = IllegalArgumentException("bad transform")
                val transformed = deferred.thenApply { throw exception }
                val thrown = assertThrows<IllegalArgumentException> { runBlocking { transformed.await() } }
                assertEquals("bad transform", thrown.message)
            }
        }
    }

    @Nested
    inner class ThenComposeTests {
        @Test
        fun `thenCompose flattens deferreds successfully`() {
            runBlocking {
                val deferred = completedDeferred(4)
                val composed = deferred.thenCompose { value ->
                    completedDeferred(value * 2)
                }
                assertEquals(8, composed.await())
            }
        }

        @Test
        fun `thenCompose propagates exception from original deferred`() {
            runBlocking {
                val exception = RuntimeException("error in original")
                val failedDeferred = CompletableDeferred<Int>()
                failedDeferred.completeExceptionally(exception)
                val composed = failedDeferred.thenCompose { value ->
                    completedDeferred(value * 2)
                }
                val thrown = assertThrows<RuntimeException> { runBlocking { composed.await() } }
                assertEquals("error in original", thrown.message)
            }
        }

        @Test
        fun `thenCompose propagates exception from inner deferred`() {
            runBlocking {
                val deferred = completedDeferred(5)
                val innerException = IllegalStateException("inner error")
                val composed = deferred.thenCompose { _ ->
                    val inner = CompletableDeferred<Int>()
                    inner.completeExceptionally(innerException)
                    inner
                }
                val thrown = assertThrows<IllegalStateException> { runBlocking { composed.await() } }
                assertEquals("inner error", thrown.message)
            }
        }

        @Test
        fun `thenCompose propagates exception thrown in function`() {
            runBlocking {
                val deferred = completedDeferred(5)
                val exception = IllegalArgumentException("bad compose")
                val composed = deferred.thenCompose<Int, Int> { throw exception }
                val thrown = assertThrows<IllegalArgumentException> { runBlocking { composed.await() } }
                assertEquals("bad compose", thrown.message)
            }
        }
    }

    @Nested
    inner class ExceptionallyTests {
        @Test
        fun `exceptionally returns original value if deferred completes normally`() {
            runBlocking {
                val deferred = completedDeferred(7)
                val handled = deferred.exceptionally { _ -> 0 }
                assertEquals(7, handled.await())
            }
        }

        @Test
        fun `exceptionally returns fallback value if deferred fails`() {
            runBlocking {
                val exception = RuntimeException("fail")
                val failedDeferred = CompletableDeferred<Int>()
                failedDeferred.completeExceptionally(exception)
                val handled = failedDeferred.exceptionally { ex ->
                    assertEquals("fail", ex.message)
                    42
                }
                assertEquals(42, handled.await())
            }
        }

        @Test
        fun `exceptionally propagates exception if fallback throws exception`() {
            runBlocking {
                val exception = RuntimeException("original")
                val failedDeferred = CompletableDeferred<Int>()
                failedDeferred.completeExceptionally(exception)
                val fallbackException = IllegalArgumentException("fallback error")
                val handled = failedDeferred.exceptionally { _ ->
                    throw fallbackException
                }
                val thrown = assertThrows<IllegalArgumentException> { runBlocking { handled.await() } }
                assertEquals("fallback error", thrown.message)
            }
        }
    }

    @Nested
    inner class ExceptionallyComposeTests {
        @Test
        fun `exceptionallyCompose returns original value if deferred completes normally`() {
            runBlocking {
                val deferred = completedDeferred(100)
                val result: Deferred<Int> = deferred.exceptionallyCompose { _ ->
                    // fallback should never be called when deferred completes normally
                    completedDeferred(0)
                }
                assertEquals(100, result.await())
            }
        }

        @Test
        fun `exceptionallyCompose returns fallback value if deferred fails`() {
            runBlocking {
                val originalException = RuntimeException("original failure")
                val failedDeferred = CompletableDeferred<Int>()
                failedDeferred.completeExceptionally(originalException)
                val result = failedDeferred.exceptionallyCompose { ex ->
                    // Optionally, verify that the exception passed to fallback is the original one
                    assertEquals("original failure", ex.message)
                    completedDeferred(42)
                }
                assertEquals(42, result.await())
            }
        }

        @Test
        fun `exceptionallyCompose propagates exception if fallback deferred fails`() {
            runBlocking {
                val originalException = RuntimeException("original failure")
                val failedDeferred = CompletableDeferred<Int>()
                failedDeferred.completeExceptionally(originalException)
                val fallbackException = IllegalStateException("fallback failure")
                val result = failedDeferred.exceptionallyCompose { _ ->
                    val fallback = CompletableDeferred<Int>()
                    fallback.completeExceptionally(fallbackException)
                    fallback
                }
                val thrown = assertThrows<IllegalStateException> { runBlocking { result.await() } }
                assertEquals("fallback failure", thrown.message)
            }
        }

        @Test
        fun `exceptionallyCompose propagates exception if fallback function throws exception`() {
            runBlocking {
                val originalException = RuntimeException("original failure")
                val failedDeferred = CompletableDeferred<Int>()
                failedDeferred.completeExceptionally(originalException)
                val fallbackException = IllegalArgumentException("fallback function exception")
                val result = failedDeferred.exceptionallyCompose<Int> { _ ->
                    throw fallbackException
                }
                val thrown = assertThrows<IllegalArgumentException> { runBlocking { result.await() } }
                assertEquals("fallback function exception", thrown.message)
            }
        }
    }

    @Nested
    @DelicateCoroutinesApi
    inner class ThenCombineTests {
        @Test
        fun `thenCombine combines two successful deferred values`() {
            runBlocking {
                val deferred1 = completedDeferred(3)
                val deferred2 = completedDeferred(4)
                val combined = deferred1.thenCombine(deferred2) { a, b -> a + b }
                assertEquals(7, combined.await())
            }
        }

        @Test
        fun `thenCombine propagates exception if first deferred fails`() {
            runBlocking {
                val exception = RuntimeException("first error")
                val failedDeferred = CompletableDeferred<Int>()
                failedDeferred.completeExceptionally(exception)
                val deferred2 = completedDeferred(4)
                val combined = failedDeferred.thenCombine(deferred2) { a, b -> a + b }
                val thrown = assertThrows<RuntimeException> { runBlocking { combined.await() } }
                assertEquals("first error", thrown.message)
            }
        }

        @Test
        fun `thenCombine propagates exception if second deferred fails`() {
            runBlocking {
                val deferred1 = completedDeferred(3)
                val exception = RuntimeException("second error")
                val failedDeferred = CompletableDeferred<Int>()
                failedDeferred.completeExceptionally(exception)
                val combined = deferred1.thenCombine(failedDeferred) { a, b -> a + b }
                val thrown = assertThrows<RuntimeException> { runBlocking { combined.await() } }
                assertEquals("second error", thrown.message)
            }
        }

        @Test
        fun `thenCombine propagates exception if combiner throws exception`() {
            runBlocking {
                val deferred1 = completedDeferred(3)
                val deferred2 = completedDeferred(4)
                val exception = IllegalStateException("combiner error")
                val combined = deferred1.thenCombine(deferred2) { _, _ -> throw exception }
                val thrown = assertThrows<IllegalStateException> { runBlocking { combined.await() } }
                assertEquals("combiner error", thrown.message)
            }
        }

        /**
         * This test ensures that [DeferredExtensions.thenCombine] is thread-safe and always completes,
         * even if one of the deferreds fails.
         */
        @RepeatedTest(10)
        fun `amplify concurrency for thenCombine to test race conditions`() {
            runBlocking {
                val NUM_PAIRS = 1000 // Increase for more stress
                val allCombines = mutableListOf<Deferred<Int>>()

                // A latch so that none of the Deferreds complete until we say "go!"
                val latch = CountDownLatch(1)

                repeat(NUM_PAIRS) {
                    val left = GlobalScope.async {
                        // Wait for the latch to ensure a mass-release
                        latch.await()
                        // Optional random delay to further scramble timings
                        delay(Random.nextLong(0, 5))
                        if (Random.nextInt(4) == 0) {
                            throw IllegalStateException("Left side fail")
                        }
                        1
                    }
                    val right = GlobalScope.async {
                        latch.await()
                        delay(Random.nextLong(0, 5))
                        if (Random.nextInt(4) == 0) {
                            throw IllegalStateException("Right side fail")
                        }
                        2
                    }
                    // Wire them up with oldThenCombine
                    val combined = left.thenCombine(right) { a, b -> a + b }
                    allCombines += combined
                }

                // Now release them all at once:
                latch.countDown()

                // We'll wait for them to finish; if one gets stuck => test eventually times out.
                // We don't care if it's a success or an exception - just that none "hang".
                allCombines.forEachIndexed { i, d ->
                    try {
                        withTimeout(1000) { d.await() }
                        // Possibly println("Pair $i succeeded: ${res}")
                    } catch (ex: TimeoutCancellationException) {
                        fail("Pair $i hung with thenCombine, big concurrency scenario!")
                    } catch (_: Throwable) {
                        // We expect random fails here, that's fine; we only worry about hangs
                    }
                }
            }
        }
    }
}
