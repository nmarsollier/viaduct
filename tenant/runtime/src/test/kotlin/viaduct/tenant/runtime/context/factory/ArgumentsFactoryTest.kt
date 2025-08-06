package viaduct.tenant.runtime.context.factory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import viaduct.api.types.Arguments

@ExperimentalCoroutinesApi
class ArgumentsFactoryTest {
    @Test
    fun noArguments() {
        assertEquals(
            Arguments.NoArguments,
            ArgumentsFactory.NoArguments.mk(MockArgs())
        )
    }

    @Test
    fun `ifClass -- missing ctor`() {
        assertNull(ArgumentsFactory.ifClass(Arguments::class))
    }

    @Test
    fun `ifClass -- valid ctor`() {
        assertNotNull(ArgumentsFactory.forClass(Foo_FieldWithArgs_Arguments::class))
    }

    @Test
    fun `forClass -- missing ctor`() {
        assertThrows<IllegalArgumentException> {
            ArgumentsFactory.forClass(Arguments::class)
        }
    }

    @Test
    fun `forClass -- NoArguments`() {
        assertEquals(
            Arguments.NoArguments,
            ArgumentsFactory.forClass(Arguments.NoArguments::class).mk(MockArgs().getArgumentsArgs())
        )
    }

    @Test
    fun `forClass -- args`() {
        val factory = ArgumentsFactory.forClass(Foo_FieldWithArgs_Arguments::class)
        val args = factory(
            MockArgs(arguments = mapOf("x" to 42, "y" to true, "z" to "Z")).getArgumentsArgs()
        )
        assertTrue(args is Foo_FieldWithArgs_Arguments)
        args as Foo_FieldWithArgs_Arguments
        assertEquals(42, args.x)
        assertEquals(true, args.y)
        assertEquals("Z", args.z)
    }
}
