package viaduct.engine.runtime.tenantloading

import graphql.schema.GraphQLInterfaceType
import viaduct.engine.api.Coordinate
import viaduct.engine.api.RawSelectionSet
import viaduct.engine.api.RequiredSelectionSet
import viaduct.engine.api.RequiredSelectionSetRegistry
import viaduct.engine.api.ViaductSchema
import viaduct.engine.runtime.select.RawSelectionSetFactoryImpl
import viaduct.engine.runtime.validation.Validator

/**
 * Validates that a graph formed by a field and its required selections contains no cycles.
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

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun validate(ctx: RequiredSelectionsValidationCtx) {
        dfs(ctx.coord, mutableListOf(), mutableSetOf(), mutableSetOf(), ctx.requiredSelectionSetRegistry)?.let { cyclePath ->
            throw RequiredSelectionsCycleException(cyclePath)
        }
    }

    /**
     * Finds cycles using depth-first search from the given [coord]
     *
     * @param coord The coordinate to validate
     * @param path The coordinates in the current path that's being searched
     * @param visiting Set version of path
     * @param visited The nodes that have been fully explored, to avoid recomputation
     */
    fun dfs(
        coord: Coordinate,
        path: MutableList<Coordinate>,
        visiting: MutableSet<Coordinate>,
        visited: MutableSet<Coordinate>,
        registry: RequiredSelectionSetRegistry
    ): List<Coordinate>? {
        // If we encounter a node currently being visited (in the current path), we have a cycle
        if (coord in visiting) {
            val cycleStart = path.indexOf(coord)
            return path.subList(cycleStart, path.size) + coord
        }

        path.add(coord)
        visiting.add(coord)

        // Explore all edges
        coord.edges(registry).forEach { edge ->
            if (edge !in visited) {
                val cycle = dfs(edge, path, visiting, visited, registry)
                if (cycle != null) return cycle
            }
        }

        path.removeLast()
        visiting.remove(coord)
        visited.add(coord)
        return null
    }

    /** return all edges referenced by this Coordinate */
    private fun Coordinate.edges(registry: RequiredSelectionSetRegistry): Set<Coordinate> {
        // if `coord` is on an interface/union, then traverse into concrete implementations of coord
        val implEdges = implementationEdges()

        // if this Coordinate has an applied selection set, then check every coord in its selection set
        val rssCoords = registry.getRequiredSelectionSets(first, second, executeAccessChecksInModstrat = true)
            .fold(emptySet<Coordinate>()) { acc, rss ->
                acc + rss.edges(registry)
            }

        return implEdges + rssCoords
    }

    /** return all Coordinates referenced by this RequiredSelectionSet */
    private fun RequiredSelectionSet.edges(registry: RequiredSelectionSetRegistry): Set<Coordinate> {
        val selectionEdges = rawSelectionSetFactory.rawSelectionSet(selections, emptyMap()).allCoords()
        val variableEdges = variablesResolvers.fold(emptySet<Coordinate>()) { acc, variablesResolver ->
            val newEdges = variablesResolver.requiredSelectionSet?.edges(registry) ?: emptySet()
            acc + newEdges
        }
        return selectionEdges + variableEdges
    }

    /** recursively return every Coordinate referenced by this [RawSelectionSet] */
    private fun RawSelectionSet.allCoords(): Set<Coordinate> {
        // start with all selections. This will include scalar fields and other selections
        // that do not support sub-selections
        val selected = selections().map { it.typeCondition to it.fieldName }.toSet()

        // "traversable" fields support sub-selections. Recurse through each traversable
        // field and extract its coords
        return traversableSelections().fold(selected) { acc, sel ->
            acc + selectionSetForField(sel.typeCondition, sel.fieldName).allCoords()
        }
    }

    /** if this Coordinate describes an abstract coordinate, return possible concrete coordinates */
    private fun Coordinate.implementationEdges(): Set<Coordinate> {
        val type = schema.schema.getType(first)
        if (type !is GraphQLInterfaceType) return emptySet()
        return schema.schema.getImplementations(type).map { impl ->
            impl.name to second
        }.toSet()
    }
}

class RequiredSelectionsCycleException(val path: List<Coordinate>) : Exception() {
    override val message: String?
        get() {
            val pathString = path.joinToString(" -> ") { "${it.first}.${it.second}" }
            return "Cyclic @Resolver selections detected in path: $pathString"
        }
}
