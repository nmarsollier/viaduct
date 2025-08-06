package viaduct.service.api.spi.mocks

import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.take
import io.kotest.property.forAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import viaduct.service.api.spi.Flag

@ExperimentalCoroutinesApi
class MockFlagManagerTest {
    @Test
    fun enabled() =
        runBlockingTest {
            Arb.flag().forAll { flag ->
                MockFlagManager.Enabled.isEnabled(flag)
            }
        }

    @Test
    fun disabled() =
        runBlockingTest {
            Arb.flag().forAll { flag ->
                !MockFlagManager.Disabled.isEnabled(flag)
            }
        }

    @Test
    fun mk() =
        runBlockingTest {
            val allFlags = Arb.flag()
            val enabledFlags = allFlags.take(100).toSet()
            val flagMgr = MockFlagManager(enabledFlags)
            allFlags.forAll { flag ->
                flagMgr.isEnabled(flag) == enabledFlags.contains(flag)
            }
        }
}

private data class MockFlag(override val flagName: String) : Flag

fun Arb.Companion.flag(): Arb<Flag> = Arb.string().map(::MockFlag)
