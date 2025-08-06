package viaduct.arbitrary.common

import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.constant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KotestPropertyBaseTest : KotestPropertyBase() {
    @Test
    fun `checkInvariants throws AssertionError on failure`() {
        assertThrows<AssertionError> {
            Arb.constant(true).checkInvariants { v, check ->
                check.isFalse(v, "test-label")
            }
        }
    }

    @Test
    fun `checkInvariants passes on success`() {
        Arb.constant(true).checkInvariants { v, check ->
            check.isTrue(v, "test-label")
        }
    }

    @Test
    fun `configures a seed from ctor`() {
        object : KotestPropertyBase(1L) {
            init {
                assertEquals(1L, PropertyTesting.defaultSeed)
                assertEquals(1L, randomSource.seed)
            }
        }
    }

    @Test
    fun `configures a default seed`() {
        object : KotestPropertyBase() {
            init {
                assertEquals(randomSource.seed, PropertyTesting.defaultSeed)
            }
        }
    }
}
