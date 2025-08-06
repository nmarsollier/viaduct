package viaduct.engine.api

import kotlin.test.assertNull
import org.junit.jupiter.api.Test

internal class NoOpCheckerExecutorFactoryImplTest {
    private val testSubject = NoOpCheckerExecutorFactoryImpl()

    @Test
    fun `Test no op functionality`() {
        val provider = testSubject.checkerExecutorForField("anyType", "anyField")
        assertNull(provider)
    }

    @Test
    fun `Test no op on node`() {
        val provider = testSubject.checkerExecutorForType("anyType")
        assertNull(provider)
    }
}
