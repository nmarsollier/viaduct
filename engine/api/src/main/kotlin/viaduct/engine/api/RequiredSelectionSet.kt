package viaduct.engine.api

import graphql.language.AstPrinter
import viaduct.graphql.utils.collectVariableReferences

/**
 * Represents a set of selections that are required.
 *
 * @param forChecker True if this is a RSS for a checker or checker variable resolver
 */
data class RequiredSelectionSet(
    val selections: ParsedSelections,
    val variablesResolvers: List<VariablesResolver>,
    val forChecker: Boolean,
    val attribution: ExecutionAttribution? = ExecutionAttribution.DEFAULT,
) {
    init {
        val refs = selections.selections.collectVariableReferences()
        val resolvers = variablesResolvers.flatMap { it.variableNames }
        val missing = refs - resolvers
        if (missing.isNotEmpty()) {
            throw UnboundVariablesException(selections, missing)
        }
    }
}

/**
 * Represents the required selection sets for a field, which includes selections for both objects and queries.
 *
 * @property objectSelections The required selection set for objects, or null if not applicable.
 * @property querySelections The required selection set for queries, or null if not applicable.
 */
data class RequiredSelectionSets(
    val objectSelections: RequiredSelectionSet?,
    val querySelections: RequiredSelectionSet?,
) {
    companion object {
        /** An empty [RequiredSelectionSets] */
        fun empty(): RequiredSelectionSets = RequiredSelectionSets(null, null)
    }
}

class UnboundVariablesException(selections: ParsedSelections, missing: Set<String>) : Exception() {
    override val message: String by lazy {
        val selectionsStr = AstPrinter.printAst(selections.selections)
        """
                |Selections contain unresolvable variable references: $missing
                |Selection set:
                |$selectionsStr
        """.trimMargin()
    }
}
