package viaduct.engine.runtime.tenantloading

import viaduct.engine.api.Coordinate
import viaduct.engine.api.NodeResolverDispatcher
import viaduct.engine.api.RequiredSelectionSet
import viaduct.engine.api.RequiredSelectionSetRegistry
import viaduct.engine.runtime.DispatcherRegistry
import viaduct.engine.runtime.validation.Validator

class NodeResolverDispatcherValidationCtx(
    val typeName: String,
    val nodeResolverDispatcher: NodeResolverDispatcher,
)

class ResolverExecutorValidationCtx(
    val coord: Coordinate,
    // Temporary solution. Will switch to use FieldResolverExecutor directly after refactoring
    // the validators to work on resolver executors instead of dispatchers.
    val objectSelectionSet: RequiredSelectionSet?,
    val querySelectionSet: RequiredSelectionSet?,
)

class RequiredSelectionsValidationCtx(
    val coord: Coordinate,
    val requiredSelectionSetRegistry: RequiredSelectionSetRegistry
)

class CheckerExecutorValidationCtx(
    val coord: Coordinate,
    val requiredSelectionSets: Map<String, RequiredSelectionSet?>,
)

/** A concrete implementation of a [Validator] for [DispatcherRegistry] */
class DispatcherRegistryValidator(
    val nodeResolverValidator: Validator<NodeResolverDispatcherValidationCtx>,
    val fieldResolverExecutorValidator: Validator<ResolverExecutorValidationCtx>,
    val requiredSelectionsValidator: Validator<RequiredSelectionsValidationCtx>,
    val checkerExecutorValidator: Validator<CheckerExecutorValidationCtx>
) : Validator<DispatcherRegistry> {
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun validate(wiring: DispatcherRegistry) {
        wiring.nodeResolverDispatchers.forEach { entry ->
            nodeResolverValidator.validate(
                NodeResolverDispatcherValidationCtx(entry.key, entry.value)
            )
        }

        wiring.fieldCheckerDispatchers.forEach { entry ->
            checkerExecutorValidator.validate(
                CheckerExecutorValidationCtx(entry.key, entry.value.requiredSelectionSets)
            )
        }

        wiring.fieldResolverDispatchers.forEach { entry ->
            fieldResolverExecutorValidator.validate(
                ResolverExecutorValidationCtx(entry.key, entry.value.objectSelectionSet, entry.value.querySelectionSet)
            )
            if (entry.value.hasRequiredSelectionSets) {
                requiredSelectionsValidator.validate(
                    RequiredSelectionsValidationCtx(
                        entry.key,
                        wiring
                    )
                )
            }
        }
    }
}
