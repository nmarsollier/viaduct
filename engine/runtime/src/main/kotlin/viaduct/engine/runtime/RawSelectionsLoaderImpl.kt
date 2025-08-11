package viaduct.engine.runtime

import viaduct.engine.api.EngineObjectData
import viaduct.engine.api.FragmentLoader
import viaduct.engine.api.RawSelectionSet
import viaduct.engine.api.RawSelectionsLoader
import viaduct.engine.api.ViaductSchema
import viaduct.engine.api.derived.DerivedFieldQueryMetadata
import viaduct.engine.api.fragment.FragmentFieldEngineResolutionResult
import viaduct.engine.api.fragment.errors.FragmentFieldEngineResolutionError
import viaduct.engine.runtime.select.RawSelectionSetImpl
import viaduct.engine.runtime.select.hash

class RawSelectionsLoaderImpl constructor(
    private val schema: ViaductSchema,
    private val fragmentLoader: FragmentLoader,
    private val mkDFPMetadata: MkDFPMetadata,
) : RawSelectionsLoader {
    class Factory(
        private val fragmentLoader: FragmentLoader,
        private val schema: ViaductSchema
    ) : RawSelectionsLoader.Factory {
        override fun forQuery(resolverId: String): RawSelectionsLoaderImpl = RawSelectionsLoaderImpl(schema, fragmentLoader, MkQueryMetadata(resolverId))

        override fun forMutation(resolverId: String): RawSelectionsLoaderImpl = RawSelectionsLoaderImpl(schema, fragmentLoader, MkMutationMetadata(resolverId))
    }

    override suspend fun load(selections: RawSelectionSet): EngineObjectData {
        val rawSS = if (!selections.isEmpty()) {
            selections as RawSelectionSetImpl
        } else {
            null
        }

        val fragResult = if (rawSS == null) {
            FragmentFieldEngineResolutionResult.empty
        } else {
            fragmentLoader.loadFromEngine(rawSS.toFragment(), mkDFPMetadata(rawSS))
        }

        // Empty data means the query failed, e.g. there was a validation error
        if (fragResult.data.isNullOrEmpty()) {
            throw loadError(fragResult.errors)
        }

        val oer = ObjectEngineResultImpl.newFromMap(
            schema.schema.getObjectType(selections.type),
            fragResult.data,
            fragResult.errors.mapNotNull { err ->
                err.cause?.let { cause ->
                    err.pathString to cause
                }
            }.toMutableList(),
            emptyList(),
            schema,
            selections
        )

        return ProxyEngineObjectData(oer, rawSS)
    }

    private fun loadError(errors: List<FragmentFieldEngineResolutionError>): RuntimeException {
        val message = "Failed to load query"
        if (errors.isEmpty()) return RuntimeException(message)

        val errorMessages = errors.map { it.message }.joinToString("; ")
        val cause = errors.firstNotNullOfOrNull { it.cause }
        return RuntimeException("$message, errors: $errorMessages", cause)
    }
}

internal typealias MkDFPMetadata = (RawSelectionSetImpl) -> DerivedFieldQueryMetadata

internal class MkQueryMetadata(private val resolverId: String) : MkDFPMetadata {
    init {
        require(resolverId.isNotEmpty()) { "resolverId may not be empty" }
    }

    override fun invoke(raw: RawSelectionSetImpl): DerivedFieldQueryMetadata =
        DerivedFieldQueryMetadata(
            queryName = "SelectionsLoader_Query_${raw.hash()}",
            // rootFieldName is not needed when onRootQuery = true
            rootFieldName = "",
            classPath = resolverId,
            providerShortClasspath = resolverId,
            onRootQuery = true,
            onRootMutation = false,
            allowMutationOnQuery = false,
            fieldOwningTenant = null,
        )
}

internal class MkMutationMetadata(private val resolverId: String) : MkDFPMetadata {
    init {
        require(resolverId.isNotEmpty()) { "resolverId may not be empty" }
    }

    override fun invoke(raw: RawSelectionSetImpl): DerivedFieldQueryMetadata =
        DerivedFieldQueryMetadata(
            queryName = "SelectionsLoader_Mutation_${raw.hash()}",
            // rootFieldName is not needed when onRootMutation = true
            rootFieldName = "",
            classPath = resolverId,
            providerShortClasspath = resolverId,
            onRootQuery = false,
            onRootMutation = true,
            allowMutationOnQuery = false,
            fieldOwningTenant = null,
        )
}
