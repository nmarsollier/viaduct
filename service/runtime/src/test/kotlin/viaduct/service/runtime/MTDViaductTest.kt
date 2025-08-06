@file:Suppress("ForbiddenImport")

package viaduct.viaduct.service.runtime

import graphql.ExecutionResultImpl
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CompletableFuture
import javax.inject.Provider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.service.api.ExecutionInput
import viaduct.service.api.Viaduct
import viaduct.service.runtime.MTDViaduct
import viaduct.service.runtime.StandardViaduct

class MTDViaductTest {
    private lateinit var provider: Provider<Viaduct>
    private lateinit var currentViaduct: StandardViaduct
    private lateinit var nextViaduct: StandardViaduct
    private lateinit var mtdViaduct: MTDViaduct

    @BeforeEach
    fun setup() {
        provider = mockk()
        currentViaduct = mockk()
        nextViaduct = mockk()
        mtdViaduct = MTDViaduct()
        mtdViaduct.viaductProvider = provider
    }

    @Test
    fun `init should set current viaduct when versions are null`() {
        val result = mtdViaduct.init(currentViaduct)

        assertTrue(result)
        assertEquals(currentViaduct, mtdViaduct.versionsRef.get().first)
        assertNull(mtdViaduct.versionsRef.get().second)
    }

    @Test
    fun `init should fail when versions are already set`() {
        mtdViaduct.init(currentViaduct)

        val result = mtdViaduct.init(nextViaduct)

        assertFalse(result)
        assertEquals(currentViaduct, mtdViaduct.versionsRef.get().first)
    }

    @Test
    fun `execute should delegate to current StandardViaduct when no hot swap is in progress`() {
        mtdViaduct.init(currentViaduct)
        val executionInput = mockk<ExecutionInput>()
        val executionResult = mockk<ExecutionResultImpl>()

        every { provider.get() } returns currentViaduct
        every { currentViaduct.execute(executionInput) } returns executionResult

        val result = mtdViaduct.execute(executionInput)

        assertEquals(executionResult, result)

        every { currentViaduct.executeAsync(executionInput) } returns CompletableFuture.completedFuture(executionResult)
        val resultAsync = mtdViaduct.executeAsync(executionInput).get()

        assertEquals(executionResult, resultAsync)
    }

    @Test
    fun `execute should delegate to current StandardViaduct if hot swap is in progress`() {
        mtdViaduct.init(currentViaduct)
        val executionInput = mockk<ExecutionInput>()
        val executionResult = mockk<ExecutionResultImpl>()

        every { provider.get() } returns nextViaduct
        every { nextViaduct.execute(executionInput) } returns executionResult

        val result = mtdViaduct.execute(executionInput)

        assertEquals(executionResult, result)

        every { nextViaduct.executeAsync(executionInput) } returns CompletableFuture.completedFuture(executionResult)
        val resultAsync = mtdViaduct.executeAsync(executionInput).get()

        assertEquals(executionResult, resultAsync)
    }

    @Test
    fun `routeUseWithCaution should return next viaduct when available`() {
        mtdViaduct.init(currentViaduct)
        mtdViaduct.beginHotSwap(nextViaduct)

        val result = mtdViaduct.routeUseWithCaution()

        assertEquals(nextViaduct, result)
    }

    @Test
    fun `routeUseWithCaution should return current viaduct when next is null`() {
        mtdViaduct.init(currentViaduct)

        val result = mtdViaduct.routeUseWithCaution()

        assertEquals(currentViaduct, result)
    }

    @Test
    fun `functions should throw if not initialized`() {
        assertThrows<IllegalStateException> {
            mtdViaduct.beginHotSwap(nextViaduct)
        }
        assertThrows<IllegalStateException> {
            mtdViaduct.endHotSwap()
        }

        val executionInput = mockk<ExecutionInput>()
        assertThrows<IllegalStateException> {
            mtdViaduct.executeAsync(executionInput)
        }
    }

    @Test
    fun `beginHotSwap should set next StandardViaduct when no hot swap in progress`() {
        mtdViaduct.init(currentViaduct)
        val canHotSwap = mtdViaduct.beginHotSwap(nextViaduct)

        assertTrue(canHotSwap)
        val versions = mtdViaduct.versionsRef.get()
        assertEquals(currentViaduct, versions.first)
        assertEquals(nextViaduct, versions.second)
    }

    @Test
    fun `beginHotSwap should not overwrite next if already in progress`() {
        mtdViaduct.init(currentViaduct)
        mtdViaduct.beginHotSwap(nextViaduct)
        val anotherViaduct = mockk<StandardViaduct>()

        val canHotSwap = mtdViaduct.beginHotSwap(anotherViaduct)

        assertFalse(canHotSwap)
        val versions = mtdViaduct.versionsRef.get()
        assertEquals(currentViaduct, versions.first)
        assertEquals(nextViaduct, versions.second)
    }

    @Test
    fun `endHotSwap should update to use next StandardViaduct`() {
        mtdViaduct.init(currentViaduct)
        mtdViaduct.beginHotSwap(nextViaduct)
        mtdViaduct.endHotSwap()

        val versions = mtdViaduct.versionsRef.get()
        assertEquals(nextViaduct, versions.first)
        assertNull(versions.second)
    }

    @Test
    fun `endHotSwap should do nothing if no hot swap in progress`() {
        mtdViaduct.init(currentViaduct)
        mtdViaduct.endHotSwap()

        val versions = mtdViaduct.versionsRef.get()
        assertEquals(currentViaduct, versions.first)
        assertNull(versions.second)
    }
}
