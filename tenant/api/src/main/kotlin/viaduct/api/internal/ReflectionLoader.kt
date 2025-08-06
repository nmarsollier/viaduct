package viaduct.api.internal

import viaduct.api.reflect.Type

interface ReflectionLoader {
    /**
     * Return a Type describing the reflected type information for the type with the provided name.
     * If no such Type information exists, a [MissingReflection] will be thrown.
     */
    fun reflectionFor(name: String): Type<*>
}

class MissingReflection(val name: String, val reason: String, cause: Throwable? = null) : Exception(cause) {
    override val message = "Missing reflection for type $name: $reason"
}
