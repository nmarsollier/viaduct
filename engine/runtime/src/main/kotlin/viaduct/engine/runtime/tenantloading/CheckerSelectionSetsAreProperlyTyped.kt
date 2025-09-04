package viaduct.engine.runtime.tenantloading

import viaduct.engine.api.ViaductSchema
import viaduct.engine.runtime.validation.Validator

/**
 * Validates that the checker required selection sets are either on field-coordinate's type
 * or on the root query type.
 */
class CheckerSelectionSetsAreProperlyTyped(
    private val schema: ViaductSchema,
) : Validator<FieldCheckerExecutorValidationCtx> {
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun validate(ctx: FieldCheckerExecutorValidationCtx) =
        ctx.run {
            val objectName = coord.first
            val rootQueryTypeName = schema.schema.queryType.name
            val mismatches = mutableSetOf<String>()

            for (rss in executor.requiredSelectionSets.values) {
                val rssName = rss?.selections?.typeName
                if (objectName != rssName && rssName != null && rssName != rootQueryTypeName) mismatches.add(rssName)
            }
            if (!mismatches.isEmpty()) {
                throw BadCheckerSelectionSetTypeException(
                    "Some selection types ($mismatches) do not match " +
                        "coordinate type or root query type."
                )
            }
        }
}

private class BadCheckerSelectionSetTypeException(msg: String) : Exception(msg)
