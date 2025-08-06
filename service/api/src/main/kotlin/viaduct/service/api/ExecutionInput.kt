package viaduct.service.api

/**
 * This is a class that encapsulates what is necessary to run execute a query.
 * For now all the data in here might be used but it should eventually have a builder
 *
 * @param query The query that will be executed
 * @param schemaId The scopedSchemaId to find the engine for
 * @param requestContext To build the ExecutionInput
 * @param variables to build the ExecutionInput
 * @param operationName to build the ExecutionInput
 */
data class ExecutionInput(
    val query: String,
    val schemaId: String,
    val requestContext: Any,
    val variables: Map<String, Any?> = emptyMap(),
    val operationName: String? = null
)
