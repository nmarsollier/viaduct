package viaduct.tenant.runtime.select

import kotlin.reflect.KClass
import viaduct.api.context.ExecutionContext
import viaduct.api.internal.internal
import viaduct.api.internal.select.SelectionsLoader
import viaduct.api.select.SelectionSet
import viaduct.api.types.CompositeOutput
import viaduct.api.types.Mutation
import viaduct.api.types.Object
import viaduct.api.types.Query
import viaduct.engine.api.RawSelectionSet
import viaduct.engine.api.RawSelectionsLoader
import viaduct.tenant.runtime.toObjectGRT

class SelectionsLoaderImpl<T : CompositeOutput>(
    private val rawSelectionsLoader: RawSelectionsLoader
) : SelectionsLoader<T> {
    class Factory(val loaderFactory: RawSelectionsLoader.Factory) : SelectionsLoader.Factory {
        override fun forQuery(resolverId: String): SelectionsLoader<Query> {
            return SelectionsLoaderImpl(loaderFactory.forQuery(resolverId))
        }

        override fun forMutation(resolverId: String): SelectionsLoader<Mutation> {
            return SelectionsLoaderImpl(loaderFactory.forMutation(resolverId))
        }
    }

    override suspend fun <U : T> load(
        ctx: ExecutionContext,
        selections: SelectionSet<U>
    ): U {
        val rawSelectionSet = selections.toRawSelectionSet()
        val proxyData = rawSelectionsLoader.load(rawSelectionSet)
        @Suppress("UNCHECKED_CAST") // toObjectGRT will enforce this cast
        return proxyData.toObjectGRT(ctx.internal, selections.type.kcls as KClass<Object>) as U
    }

    private fun SelectionSet<*>.toRawSelectionSet(): RawSelectionSet {
        val impl = this as? SelectionSetImpl ?: return RawSelectionSet.empty(this.type.name)
        return impl.rawSelectionSet
    }
}
