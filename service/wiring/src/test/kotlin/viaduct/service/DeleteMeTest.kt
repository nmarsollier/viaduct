package viaduct.service

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class DeleteMeTest {
    @Test
    fun `when ask for greeting then Brain speaks`() {
        val actual = DeleteMe().greeting
        assertEquals("Hello, World!", actual)
    }
}
