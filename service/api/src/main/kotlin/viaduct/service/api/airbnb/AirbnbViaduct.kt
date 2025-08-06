package viaduct.service.api.airbnb

import graphql.GraphQL
import viaduct.engine.api.EngineExecutionContext
import viaduct.service.api.Viaduct

interface AirbnbViaduct : Viaduct {
    /** Return the graphql-java engine that powers a given schema. */
    fun getEngine(schemaId: String): GraphQL?

    fun mkEngineExecutionContext(): EngineExecutionContext
}
