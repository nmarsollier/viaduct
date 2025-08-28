package viaduct.engine.runtime.tenantloading

import viaduct.engine.api.Coordinate

class InvalidVariableException(
    val coord: Coordinate,
    val variableName: String,
    val reason: String
) : Exception() {
    override val message: String = "Invalid configuration for coordinate `$coord` variable `$variableName`: $reason"
}
