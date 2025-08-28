package viaduct.engine.api.observability

import viaduct.engine.api.ExecutionAttribution

/**
 * Context for observability.
 * This context is used to provide information about the execution that can be used by instrumentations (for tracing or metrics).
 *
 * @property attribution The attribution of the current execution.
 */
data class ExecutionObservabilityContext(
    val attribution: ExecutionAttribution?
)
