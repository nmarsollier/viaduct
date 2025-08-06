package viaduct.engine.runtime

import javax.inject.Singleton
import viaduct.engine.api.CheckerExecutor
import viaduct.engine.api.Coordinate
import viaduct.engine.api.FieldCheckerDispatcherRegistry
import viaduct.engine.api.FieldResolverDispatcher
import viaduct.engine.api.FieldResolverDispatcherRegistry
import viaduct.engine.api.NodeResolverDispatcher
import viaduct.engine.api.NodeResolverDispatcherRegistry
import viaduct.engine.api.RequiredSelectionSet
import viaduct.engine.api.RequiredSelectionSetRegistry
import viaduct.engine.api.TypeCheckerDispatcherRegistry

@Singleton
class DispatcherRegistry(
    internal val fieldResolverDispatchers: Map<Coordinate, FieldResolverDispatcher>,
    internal val nodeResolverDispatchers: Map<String, NodeResolverDispatcher>,
    internal val checkerExecutors: Map<Coordinate, CheckerExecutor>,
    internal val nodeCheckerExecutors: Map<String, CheckerExecutor>
) : RequiredSelectionSetRegistry, NodeResolverDispatcherRegistry, TypeCheckerDispatcherRegistry, FieldResolverDispatcherRegistry, FieldCheckerDispatcherRegistry {
    override fun getFieldResolverDispatcher(
        typeName: String,
        fieldName: String
    ) = fieldResolverDispatchers[Pair(typeName, fieldName)]

    override fun getRequiredSelectionSets(
        typeName: String,
        fieldName: String,
        executeAccessChecksInModstrat: Boolean
    ): List<RequiredSelectionSet> =
        buildList {
            val fieldResolverExecutor = getFieldResolverDispatcher(typeName, fieldName)
            fieldResolverExecutor?.let {
                if (it.objectSelectionSet != null) {
                    add(it.objectSelectionSet!!)
                }
                if (it.querySelectionSet != null) {
                    add(it.querySelectionSet!!)
                }
            }
            // Register checker RSSs when:
            // 1. all checker execution is running in modstrat, or
            // 2. if checker is present for a modern field. In this case, even if the checker is still executed
            //  in resolver data fetcher, we need to register its RSS to query plan to get correct OER
            //  for checker execution.
            if (executeAccessChecksInModstrat || fieldResolverExecutor != null) {
                getCheckerExecutor(typeName, fieldName)?.requiredSelectionSets?.values?.filterNotNull()?.let { addAll(it) }
            }
        }

    override fun getCheckerExecutor(
        typeName: String,
        fieldName: String
    ) = checkerExecutors[Pair(typeName, fieldName)]

    override fun getNodeResolverDispatcher(typeName: String): NodeResolverDispatcher? = nodeResolverDispatchers[typeName]

    override fun getTypeCheckerExecutor(typeName: String): CheckerExecutor? = nodeCheckerExecutors[typeName]

    // Visible for testing
    fun get() = fieldResolverDispatchers

    companion object {
        /** An [DispatcherRegistry] that returns null for every request. */
        val Empty = DispatcherRegistry(emptyMap(), emptyMap(), emptyMap(), emptyMap())
    }

    fun isEmpty() = fieldResolverDispatchers.isEmpty() && nodeResolverDispatchers.isEmpty() && checkerExecutors.isEmpty() && nodeCheckerExecutors.isEmpty()
}
