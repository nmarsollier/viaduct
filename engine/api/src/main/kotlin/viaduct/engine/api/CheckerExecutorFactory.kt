package viaduct.engine.api

interface CheckerExecutorFactory {
    fun checkerExecutorForField(
        typeName: String,
        fieldName: String
    ): CheckerExecutor?

    fun checkerExecutorForType(typeName: String): CheckerExecutor?
}
