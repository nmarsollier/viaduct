package viaduct.engine.runtime.tenantloading

import viaduct.engine.api.CheckerExecutor
import viaduct.engine.api.Coordinate
import viaduct.engine.api.FieldResolverExecutor
import viaduct.engine.api.NodeResolverExecutor
import viaduct.engine.api.RequiredSelectionSetRegistry
import viaduct.engine.runtime.DispatcherRegistry
import viaduct.engine.runtime.validation.Validator

/** A concrete implementation of a [Validator] for [DispatcherRegistry] */
class ExecutorValidator(
    val nodeResolverValidator: Validator<NodeResolverExecutorValidationCtx>,
    val fieldResolverExecutorValidator: Validator<FieldResolverExecutorValidationCtx>,
    val requiredSelectionsValidator: Validator<RequiredSelectionsValidationCtx>,
    val fieldCheckerExecutorValidator: Validator<FieldCheckerExecutorValidationCtx>
) : Validator<ExecutorValidatorContext> {
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun validate(ctx: ExecutorValidatorContext) {
        ctx.nodeResolverExecutors.forEach { (typeName, executor) ->
            nodeResolverValidator.validate(
                NodeResolverExecutorValidationCtx(typeName, executor)
            )
        }

        ctx.fieldCheckerExecutors.forEach { (coord, executor) ->
            fieldCheckerExecutorValidator.validate(
                FieldCheckerExecutorValidationCtx(coord, executor)
            )
        }

        ctx.fieldResolverExecutors.forEach { (coord, executor) ->
            fieldResolverExecutorValidator.validate(
                FieldResolverExecutorValidationCtx(coord, executor)
            )
            if (executor.hasRequiredSelectionSets()) {
                requiredSelectionsValidator.validate(
                    RequiredSelectionsValidationCtx(
                        coord,
                        ctx.requiredSelectionSetRegistry
                    )
                )
            }
        }
    }
}

data class ExecutorValidatorContext(
    val fieldResolverExecutors: Map<Coordinate, FieldResolverExecutor>,
    val nodeResolverExecutors: Map<String, NodeResolverExecutor>,
    val fieldCheckerExecutors: Map<Coordinate, CheckerExecutor>,
    val requiredSelectionSetRegistry: RequiredSelectionSetRegistry
)

data class NodeResolverExecutorValidationCtx(
    val typeName: String,
    val executor: NodeResolverExecutor,
)

data class FieldResolverExecutorValidationCtx(
    val coord: Coordinate,
    val executor: FieldResolverExecutor,
)

data class RequiredSelectionsValidationCtx(
    val coord: Coordinate,
    val requiredSelectionSetRegistry: RequiredSelectionSetRegistry
)

data class FieldCheckerExecutorValidationCtx(
    val coord: Coordinate,
    val executor: CheckerExecutor
)
