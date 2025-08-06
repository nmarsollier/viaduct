package viaduct.service.api

import graphql.ExecutionResult
import graphql.schema.GraphQLSchema
import java.util.concurrent.CompletableFuture

/**
 * A unified interface for configuring and executing queries against the Viaduct runtime
 */
interface Viaduct {
    /**
     *  Executes a query for the schema registry by using a Viaduct ExecutionInput and wraps it on a CompletableFuture.
     *  @param executionInput THe execution Input
     *  @return the CompletableFuture of ExecutionResult who contains the sorted results or the error which was produced
     */
    fun executeAsync(executionInput: ExecutionInput): CompletableFuture<ExecutionResult>

    /**
     *  Executes a query for the schema registry by using a Viaduct ExecutionInput.
     *  @param executionInput THe execution Input
     *  @return the ExecutionResult who contains the sorted results
     */
    fun execute(executionInput: ExecutionInput): ExecutionResult

    /**
     * This function is used to get the applied scopes for a given schemaId
     *
     * @param schemaId the id of the schema for which we want a [GraphQLSchema]
     *
     * @return Set of scopes that are applied to the schema
     */
    fun getAppliedScopes(schemaId: String): Set<String>?

    /**
     * Temporary - Will be either private/or somewhere not exposed
     *
     * This function is used to get the GraphQLSchema from the registered schemas.
     * Returns null if no such schema is registered.
     *
     * @param schemaId the id of the schema for which we want a [GraphQLSchema]
     *
     * @return GraphQLSchema instance of the registered scope
     */
    @Deprecated("Will be either private/or somewhere not exposed")
    fun getSchema(schemaId: String): GraphQLSchema?
}
