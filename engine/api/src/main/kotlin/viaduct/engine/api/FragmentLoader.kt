package viaduct.engine.api

import graphql.schema.DataFetchingEnvironment
import viaduct.engine.api.derived.DerivedFieldQueryMetadata
import viaduct.engine.api.fragment.Fragment
import viaduct.engine.api.fragment.FragmentFieldEngineResolutionResult

interface FragmentLoader {
    suspend fun loadFromEngine(
        fragment: Fragment,
        metadata: DerivedFieldQueryMetadata,
        source: Any? = null,
        dataFetchingEnvironment: DataFetchingEnvironment? = null
    ): FragmentFieldEngineResolutionResult

    suspend fun loadEngineObjectData(
        fragment: Fragment,
        metadata: DerivedFieldQueryMetadata,
        source: Any,
        dataFetchingEnvironment: DataFetchingEnvironment
    ): Any {
        throw NotImplementedError("loadEngineObjectData is not implemented for FragmentLoader")
    }
}
