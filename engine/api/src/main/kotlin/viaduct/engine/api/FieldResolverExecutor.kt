package viaduct.engine.api

/**
 * Executor for a tenant-written resolver function.
 */
interface FieldResolverExecutor {
    /** The required selection set for the resolver */
    val objectSelectionSet: RequiredSelectionSet?

    /** The query selection set for the resolver **/
    val querySelectionSet: RequiredSelectionSet?

    /** Tenant-digestible metadata associated with this particular resolver */
    val metadata: Map<String, String>

    /**
     * Executes this resolver. Performs the wrapping and unwrapping of EngineObjectData
     * when passing it between the tenant and the engine.
     *
     * @param arguments The arguments for the field being resolved
     * @param objectValue The result of executing the required selection set
     * @param selections The selections on the field being resolved, as requested by
     *  the caller of this resolver, null if type does not support selections. Usually
     *  used by tenants to examine what the client is querying
     * @param context The [EngineExecutionContext] for this request
     * @return The unwrapped (untyped) result
     */
    suspend fun resolve(
        arguments: Map<String, Any?>,
        objectValue: EngineObjectData,
        queryValue: EngineObjectData,
        selections: RawSelectionSet?,
        context: EngineExecutionContext,
    ): Any?

    /**
     * Returns true if this resolver has a required selection set, either on the parent object or on Query.
     */
    fun hasRequiredSelectionSets() = objectSelectionSet != null || querySelectionSet != null
}
