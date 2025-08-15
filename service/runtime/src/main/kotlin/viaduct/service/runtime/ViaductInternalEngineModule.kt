package viaduct.service.runtime

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import graphql.execution.DataFetcherExceptionHandler
import viaduct.engine.api.CheckerExecutorFactory
import viaduct.engine.api.NoOpCheckerExecutorFactoryImpl
import viaduct.engine.api.coroutines.CoroutineInterop
import viaduct.engine.api.execution.ResolverErrorBuilder
import viaduct.engine.api.execution.ResolverErrorReporter
import viaduct.engine.api.fragment.ExecutableFragmentParser
import viaduct.engine.api.fragment.ViaductExecutableFragmentParser
import viaduct.engine.runtime.execution.ViaductDataFetcherExceptionHandler
import viaduct.service.api.spi.FlagManager

/**
 * Injects the internal engine needed to run Viaduct.
 * It injects instrumentation and the scoped Schemas/ documentProvider Map and the GraphqlSchemaRegistry from the Builder.
 * These modules are the ones needed the inject Viaduct, and get it by the injector that is created from this module.
 */
internal class ViaductInternalEngineModule(
    val schemaRegistry: ViaductSchemaRegistry,
    val flagManager: FlagManager?,
    val coroutineInterop: CoroutineInterop,
    val checkerExecutorFactory: CheckerExecutorFactory?,
    val dataFetcherExceptionHandler: DataFetcherExceptionHandler?,
    val resolverErrorReporter: ResolverErrorReporter,
    val resolverErrorBuilder: ResolverErrorBuilder
) : AbstractModule() {
    override fun configure() {
        bind(ExecutableFragmentParser::class.java).to(ViaductExecutableFragmentParser::class.java)
    }

    @Provides
    @Singleton
    fun providerScopedFuture(): CoroutineInterop {
        return coroutineInterop
    }

    @Provides
    @Singleton
    fun providesFlagManager(): FlagManager {
        return flagManager ?: FlagManager.default
    }

    @Provides
    @Singleton
    fun providesCheckerExecutorFactory(): CheckerExecutorFactory {
        return checkerExecutorFactory ?: NoOpCheckerExecutorFactoryImpl()
    }

    @Provides
    @Singleton
    fun providesSchemaRegistry(): ViaductSchemaRegistry = schemaRegistry

    @Provides
    @Singleton
    fun providesDataFetcherExceptionHandler(): DataFetcherExceptionHandler {
        return dataFetcherExceptionHandler ?: ViaductDataFetcherExceptionHandler(resolverErrorReporter, resolverErrorBuilder)
    }
}
