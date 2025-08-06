package viaduct.tenant.runtime.context.factory

import viaduct.api.internal.select.SelectionsLoader
import viaduct.api.types.Mutation
import viaduct.api.types.Query

class SelectionsLoaderArgs(
    val resolverId: String,
    /** A service-scoped [SelectionsLoader.Factory] */
    val selectionsLoaderFactory: SelectionsLoader.Factory,
)

/** factory methods for Factory<Args, SelectionsLoader<*>> */
object SelectionsLoaderFactory {
    /** a standard Factory for SelectionsLoader<Query> */
    val forQuery: Factory<SelectionsLoaderArgs, SelectionsLoader<Query>> =
        Factory { args ->
            args.selectionsLoaderFactory.forQuery(args.resolverId)
        }

    /** a standard Factory for SelectionsLoader<Mutation> */
    val forMutation: Factory<SelectionsLoaderArgs, SelectionsLoader<Mutation>> =
        Factory { args ->
            args.selectionsLoaderFactory.forMutation(args.resolverId)
        }
}
