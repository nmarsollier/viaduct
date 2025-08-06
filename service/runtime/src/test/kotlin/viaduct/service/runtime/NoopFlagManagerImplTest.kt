package viaduct.service.runtime

import io.mockk.mockk
import org.junit.jupiter.api.Test
import viaduct.service.api.spi.FlagManager.Companion.NoOpFlagManager

class NoopFlagManagerImplTest {
    @Test
    fun `noopflag manager impl will always bring false`() {
        val flagManager = NoOpFlagManager
        assert(!flagManager.isEnabled(mockk()))
    }
}
