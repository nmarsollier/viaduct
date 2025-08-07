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
        /** breadth-first iteration, returning the path of the first cycle found */
        tailrec fun loop(
            path: List<Coordinate>,
            pending: Set<Coordinate>
        ): List<Coordinate>? {
            if (pending.isEmpty()) return null
            val coord = pending.first()

            // check coord against just the first element. This is sufficient to catch cycles
            // between required selection sets, while allowing for non-cyclic co-recursive objects
            if (path.isNotEmpty() && coord == path[0]) {
                // cycle found, append to path and return
                return path + coord
            }

            val coordEdges = coord.edges(ctx.requiredSelectionSetRegistry)
            return loop(
                path = path + coord,
                pending = (pending.drop(1) + coordEdges).toSet()
            )
        }

        loop(emptyList(), setOf(ctx.coord))?.let { cyclePath ->
            throw RequiredSelectionsCycleException(cyclePath)
        }
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
