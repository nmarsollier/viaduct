package viaduct.tenant.runtime.context.factory

/**
 * A Factory is an interface for constructing the context objects used
 * during Viaduct resolver execution. This interface can be used to add a layer of
 * abstraction and customization over how contexts are created.
 */
fun interface Factory<in A, out T> {
    /** @see [invoke] */
    fun mk(args: A): T

    /** construct a [T] from the provided [A] */
    operator fun invoke(args: A): T = mk(args)

    companion object {
        /** create a Factory that returns a constant value */
        fun <T> const(t: T): Factory<Any, T> = Factory { _ -> t }
    }
}
