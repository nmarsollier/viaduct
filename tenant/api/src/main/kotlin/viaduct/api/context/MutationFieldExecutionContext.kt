package viaduct.api.context

import viaduct.api.select.SelectionSet
import viaduct.api.types.Arguments
import viaduct.api.types.CompositeOutput
import viaduct.api.types.Mutation
import viaduct.api.types.Object
import viaduct.api.types.Query

/** An [ExecutionContext] provided to mutation resolvers */
interface MutationFieldExecutionContext<
    T : Object,
    Q : Query,
    A : Arguments,
    O : CompositeOutput
> : FieldExecutionContext<T, Q, A, O> {
    /** load the provided [SelectionSet] and return the response */
    suspend fun <T : Mutation> mutation(selections: SelectionSet<T>): T
}
