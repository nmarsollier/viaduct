package viaduct.api

interface DynamicOutputValueBuilder<T> {
    fun put(
        name: String,
        value: Any?
    ): DynamicOutputValueBuilder<T>

    fun build(): T
}
