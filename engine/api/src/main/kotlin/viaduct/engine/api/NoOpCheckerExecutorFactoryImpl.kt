package viaduct.engine.api

import com.google.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("ktlint:standard:indent")
class NoOpCheckerExecutorFactoryImpl
    @Inject
    constructor() : CheckerExecutorFactory {
        override fun checkerExecutorForField(
            typeName: String,
            fieldName: String
        ): CheckerExecutor? = null

        override fun checkerExecutorForType(typeName: String): CheckerExecutor? = null
    }
