package viaduct.engine.runtime.tenantloading

import viaduct.engine.api.ViaductSchema
import viaduct.engine.runtime.validation.Validator

/**
 * Validates that the required selection sets on a resolver are on the correct type,
 * and are valid for that type.  Specifically, objectSelections must be on the
 * field-coordinate's type and querySelections on the query type.
 */
class CheckerSelectionSetsAreProperlyTyped(
    private val schema: ViaductSchema,
) : Validator<CheckerExecutorValidationCtx> {
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun validate(ctx: CheckerExecutorValidationCtx) =
        ctx.run {
            val objectName = coord.first
            val mismatches = mutableSetOf<String>()

            for (rss in executor.requiredSelectionSets.values) {
                val rssName = rss?.selections?.typeName
                if (objectName != rssName && rssName != null) mismatches.add(rssName)
            }
            if (!mismatches.isEmpty()) {
                throw BadCheckerSelectionSetTypeException("Some selection types ($mismatches) do not match coordinate type ($objectName).")
            }
        }
}

private class BadCheckerSelectionSetTypeException(msg: String) : Exception(msg)
