package viaduct.engine.api

/**
 * Class that delegates to a data loader to call [FieldResolverExecutor]
 */
interface FieldResolverDispatcher {
    /** The required selection set for the resolver */
    val objectSelectionSet: RequiredSelectionSet?

    /** The query selection set for the resolver **/
    val querySelectionSet: RequiredSelectionSet?

    val hasRequiredSelectionSets: Boolean

    /** The metadata associated with this resolver **/
    val resolverMetadata: ResolverMetadata

    suspend fun resolve(
        arguments: Map<String, Any?>,
        objectValue: EngineObjectData,
        queryValue: EngineObjectData,
        selections: RawSelectionSet?,
        context: EngineExecutionContext,
    ): Any?
}
