package viaduct.api

import viaduct.utils.api.InternalApi

/**
 * Builder interface for dynamic output values.
 *
 * This is not meant to be used directly, but rather through [viaduct.api.internal.ViaductObjectBuilder.dynamicBuilderFor].
 */
@InternalApi
interface DynamicOutputValueBuilder<T> {
    fun put(
        name: String,
        value: Any?
    ): DynamicOutputValueBuilder<T>

    fun build(): T
}
