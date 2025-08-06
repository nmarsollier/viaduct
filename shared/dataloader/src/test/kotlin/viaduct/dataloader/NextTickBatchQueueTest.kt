package viaduct.dataloader

import kotlin.test.assertEquals
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test

@Suppress("DEPRECATION")
@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
class NextTickBatchQueueTest {
    private val debugInfo = NextTickMetadata(
        "someName",
        100,
        1000
    )

    @Test
    fun testEnqueueAndFlush() =
        runBlockingTest {
            val queue = NextTickBatchQueue { flushedItems, dispatchingContext ->
                assertEquals(listOf("Hello"), flushedItems)
                assertEquals(debugInfo, dispatchingContext.metadata)
            }

            queue.enqueue("Hello")
            queue.flush(debugInfo)
        }

    @Test
    fun testEnqueueAndFlushAsync() =
        runBlockingTest {
            val queue = NextTickBatchQueue { flushedItems, dispatchingContext ->
                assertEquals(setOf("Here", "What"), flushedItems.toSet())
                assertEquals(debugInfo, dispatchingContext.metadata)
            }

            val what = async { queue.enqueue("What") }
            val here = async { queue.enqueue("Here") }
            what.await()
            here.await()
            queue.flush(debugInfo)
        }

    @Test
    fun testMultipleRounds() =
        runBlockingTest {
            val queue = NextTickBatchQueue { flushedItems, context ->
                if (context.tickIndex == 0) {
                    assertEquals(setOf("Here", "What"), flushedItems.toSet())
                } else if (context.tickIndex == 1) {
                    assertEquals(setOf("More", "Rounds"), flushedItems.toSet())
                }
            }

            val what = async { queue.enqueue("What") }
            val here = async { queue.enqueue("Here") }
            what.await()
            here.await()
            queue.flush(debugInfo)

            val more = async { queue.enqueue("More") }
            val rounds = async { queue.enqueue("Rounds") }
            more.await()
            rounds.await()
            queue.flush(debugInfo)
        }
}
