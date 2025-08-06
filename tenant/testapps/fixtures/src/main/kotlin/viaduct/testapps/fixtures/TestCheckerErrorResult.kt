package viaduct.testapps.fixtures

import viaduct.engine.api.CheckerResult
import viaduct.engine.api.CheckerResultContext

internal class TestCheckerErrorResult(override val error: Exception) : CheckerResult.Error {
    override fun isErrorForResolver(ctx: CheckerResultContext): Boolean {
        return true
    }

    override fun combine(fieldResult: CheckerResult.Error): CheckerResult.Error {
        return fieldResult
    }
}
