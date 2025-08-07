package viaduct.engine.api

/**
 * Executor for a tenant-written resolver function.
 */
interface FieldResolverExecutor {
    /** The required selection set for the resolver */
    val objectSelectionSet: RequiredSelectionSet?

    /** The query selection set for the resolver **/
    val querySelectionSet: RequiredSelectionSet?

    /** Same as field coordinate. Uniquely identifies a resolver function **/
    val resolverId: String

    /** Tenant-digestible metadata associated with this particular resolver */
    val metadata: Map<String, String>

    /**
     * The input for a single node in the batch
     *
     * @param arguments The arguments for the field being resolved
     * @param objectValue The result of executing the required selection set
     * @param queryValue The result of executing the query selection set
     * @param selections The selections on the field being resolved, as requested by
     * the caller of this resolver, null if type does not support selections. Usually
     * used by tenants to examine what the client is querying
     */
    data class Selector(
        val arguments: Map<String, Any?>,
        val objectValue: EngineObjectData,
        val queryValue: EngineObjectData,
        val selections: RawSelectionSet?
    )

    /**
     * Whether or not this resolver supports batch resolution.
     * If true, the resolver can be called with a list of selectors.
     * If false, the resolver must be called with a single selector.
     */
    val isBatching: Boolean

    /**
     * Returns true if this resolver has a required selection set, either on the parent object or on Query.
     */
    fun hasRequiredSelectionSets() = objectSelectionSet != null || querySelectionSet != null

    /**
     * Resolves a list of selectors in a batch if isBatching is true.
     * If isBatching is false, it enforces the selectors list size to be 1.
     *
     * @param selector The input to resolve
     * @param context The execution context for the resolver
     * @return A map of selectors to their resolved results.
     */
    suspend fun batchResolve(
        selectors: List<Selector>,
        context: EngineExecutionContext
    ): Map<Selector, Result<Any?>>
}
