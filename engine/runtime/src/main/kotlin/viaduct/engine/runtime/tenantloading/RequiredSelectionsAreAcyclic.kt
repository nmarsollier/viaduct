package viaduct.engine.runtime.tenantloading

import graphql.schema.GraphQLInterfaceType
import kotlin.collections.plus
import viaduct.engine.api.Coordinate
import viaduct.engine.api.RawSelectionSet
import viaduct.engine.api.RequiredSelectionSet
import viaduct.engine.api.RequiredSelectionSetRegistry
import viaduct.engine.api.ViaductSchema
import viaduct.engine.runtime.select.RawSelectionSetFactoryImpl
import viaduct.engine.runtime.validation.Validator

/**
 * Validates that a graph formed by required selection sets contains no cycles. In this graph,
 * there's an edge from RSS V to W if W is a RSS for a field coordinate in V's selections.
 * Checker RSS's only depend on resolver RSS's, whereas resolver RSS's depend on both checker
 * and resolver RSS's.
 *
 * This validator biases towards strict cycle validation and will declare some topologies
 * to be cyclic even if they are potentially acyclic at runtime.
 *
 * For example, consider this recursive type that is mediated by an interface:
 *   ```graphql
 *     interface Interface { x: Int }
 *     type Obj implements Interface { x: Int, iface: Interface }
 *   ```
 *     Where Obj.x requires "{ iface { x } }"
 *
 * Even though this graph is only cyclic when the concrete type of `Obj.iface` is Obj, this
 * Validator declares this topology to be a cycle and always invalid.
 */
class RequiredSelectionsAreAcyclic(
    private val schema: ViaductSchema,
) : Validator<RequiredSelectionsValidationCtx> {
    private val rawSelectionSetFactory = RawSelectionSetFactoryImpl(schema)

    /**
     * Validates all RSS's of the given field.
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun validate(ctx: RequiredSelectionsValidationCtx) {
        val registry = ctx.requiredSelectionSetRegistry
        val coord = ctx.coord
        registry.getFieldResolverRequiredSelectionSets(coord.first, coord.second).forEach { rss ->
            validate(FieldResolverRSSNode(rss, coord), registry)
        }
        registry.getFieldCheckerRequiredSelectionSets(coord.first, coord.second, executeAccessChecksInModstrat = true).forEach { rss ->
            validate(FieldCheckerRSSNode(rss, coord), registry)
        }
    }

    private fun validate(
        root: RSSNode,
        registry: RequiredSelectionSetRegistry
    ) {
        dfs(root, mutableListOf(), mutableSetOf(), mutableSetOf(), registry)?.let { cyclePath ->
            throw RequiredSelectionsCycleException(cyclePath.map { it.toString() })
        }
    }

    /**
     * Finds cycles using depth-first search from the given [node]
     *
     * @param node The node to search for cycles
     * @param path The nodes in the path that's being searched
     * @param visiting The set version of [path]
     * @param visited The nodes that have been fully explored, to avoid recomputation
     */
    private fun dfs(
        node: RSSNode,
        path: MutableList<RSSNode>,
        visiting: MutableSet<RSSNode>,
        visited: MutableSet<RSSNode>,
        registry: RequiredSelectionSetRegistry
    ): List<RSSNode>? {
        // If we encounter a node currently being visited (in the current path), we have a cycle
        if (node in visiting) {
            val cycleStart = path.indexOf(node)
            return path.subList(cycleStart, path.size) + node
        }

        path.add(node)
        visiting.add(node)

        node.edges(registry).forEach { edge ->
            if (edge !in visited) {
                val cycle = dfs(edge, path, visiting, visited, registry)
                if (cycle != null) return cycle
            }
        }

        path.removeLast()
        visiting.remove(node)
        visited.add(node)
        return null
    }

    /**
     * Represents a RequiredSelectionSet for a field
     */
    private abstract inner class RSSNode(
        protected val rss: RequiredSelectionSet,
        protected val coordinate: Coordinate
    ) {
        /**
         * Returns the edges from this node as a set of destination nodes
         */
        final fun edges(registry: RequiredSelectionSetRegistry): List<RSSNode> {
            val coords = rss.objectCoords(registry)
            return buildList {
                coords.forEach { coord ->
                    addAll(fieldEdges(registry, coord))
                }
            }
        }

        /**
         * Returns the edges contributed by the given field coordinate
         */
        protected abstract fun fieldEdges(
            registry: RequiredSelectionSetRegistry,
            coordinate: Coordinate
        ): List<RSSNode>

        /**
         * Returns all field coordinates referenced by this [RequiredSelectionSet]. All interface coordinates are
         * replaced by coordinates of its object type implementations.
         */
        private fun RequiredSelectionSet.objectCoords(registry: RequiredSelectionSetRegistry): Set<Coordinate> {
            val selectionCoords = rawSelectionSetFactory.rawSelectionSet(selections, emptyMap()).objectCoords()
            val variableCoords = variablesResolvers.fold(emptySet<Coordinate>()) { acc, variablesResolver ->
                val newCoords = variablesResolver.requiredSelectionSet?.objectCoords(registry) ?: emptySet()
                acc + newCoords
            }
            return selectionCoords + variableCoords
        }

        /**
         * Returns all field coordinates referenced by this [RawSelectionSet]. All interface coordinates are
         * replaced by coordinates of its object type implementations.
         */
        private fun RawSelectionSet.objectCoords(): Set<Coordinate> =
            buildSet {
                // start with all selections. This will include scalar fields and other selections
                // that do not support sub-selections
                selections().forEach {
                    implementations(it.typeCondition)?.let { implementations ->
                        implementations.map { objectTypeName -> add(objectTypeName to it.fieldName) }
                    } ?: add(it.typeCondition to it.fieldName)
                }
                // "traversable" fields support sub-selections. Recurse through each traversable
                // field and extract its coords
                traversableSelections().forEach { sel ->
                    addAll(selectionSetForField(sel.typeCondition, sel.fieldName).objectCoords())
                }
            }

        /**
         * If [typeName] is an interface, returns the names of all object types that implement it.
         * Otherwise returns null.
         */
        private fun implementations(typeName: String): List<String>? {
            val type = schema.schema.getType(typeName)
            if (type !is GraphQLInterfaceType) return null
            return schema.schema.getImplementations(type).map { it.name }
        }

        override fun hashCode(): Int {
            return 31 * rss.hashCode() + coordinate.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return javaClass == other?.javaClass &&
                other is RSSNode &&
                coordinate == other.coordinate &&
                rss == other.rss
        }

        override fun toString(): String {
            return "${coordinate.first}.${coordinate.second}"
        }
    }

    /**
     * A field resolver's RSS
     */
    private inner class FieldResolverRSSNode(
        rss: RequiredSelectionSet,
        coordinate: Coordinate,
    ) : RSSNode(rss, coordinate) {
        override fun fieldEdges(
            registry: RequiredSelectionSetRegistry,
            coordinate: Coordinate
        ): List<RSSNode> =
            buildList {
                val resolverRequiredSelections = registry.getFieldResolverRequiredSelectionSets(coordinate.first, coordinate.second)
                resolverRequiredSelections.forEach {
                    add(FieldResolverRSSNode(it, coordinate))
                }
                val checkerRequiredSelections = registry.getFieldCheckerRequiredSelectionSets(coordinate.first, coordinate.second, executeAccessChecksInModstrat = true)
                checkerRequiredSelections.forEach {
                    add(FieldCheckerRSSNode(it, coordinate))
                }
            }
    }

    /**
     * A field checker's RSS
     */
    private inner class FieldCheckerRSSNode(
        rss: RequiredSelectionSet,
        coordinate: Coordinate
    ) : RSSNode(rss, coordinate) {
        override fun fieldEdges(
            registry: RequiredSelectionSetRegistry,
            coordinate: Coordinate
        ): List<RSSNode> {
            val resolverRequiredSelections = registry.getFieldResolverRequiredSelectionSets(coordinate.first, coordinate.second)
            return resolverRequiredSelections.map { FieldResolverRSSNode(it, coordinate) }
        }
    }
}

class RequiredSelectionsCycleException(val path: List<String>) : Exception() {
    override val message: String?
        get() {
            val pathString = path.joinToString(" -> ") { it }
            return "Cyclic @Resolver selections detected in path: $pathString"
        }
}
