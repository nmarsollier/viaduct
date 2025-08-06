package viaduct.testapps.fixtures

import com.airbnb.viaduct.errors.ViaductFailedToPerformPolicyCheckException
import com.airbnb.viaduct.errors.ViaductPermissionDeniedException
import viaduct.engine.api.CheckerExecutor
import viaduct.engine.api.CheckerResult
import viaduct.engine.api.EngineExecutionContext
import viaduct.engine.api.EngineObjectData
import viaduct.engine.api.RequiredSelectionSet

internal class TestAppCheckerExecutorImpl(
    private val canSee: Boolean?,
    override val requiredSelectionSets: Map<String, RequiredSelectionSet?> = emptyMap()
) : CheckerExecutor {
    override suspend fun execute(
        arguments: Map<String, Any?>,
        objectDataMap: Map<String, EngineObjectData>,
        context: EngineExecutionContext
    ): CheckerResult {
        try {
            testExecute(canSee)
        } catch (e: Exception) {
            return TestCheckerErrorResult(e)
        }
        return CheckerResult.Success
    }

    private fun testExecute(canAccess: Boolean?) {
        when (canAccess) {
            null -> throw ViaductFailedToPerformPolicyCheckException("canAccess")
            false -> throw ViaductPermissionDeniedException(
                message = "This field is not accessible",
            )
            true -> { /* Continue execution - permission granted */ }
        }
    }
}
