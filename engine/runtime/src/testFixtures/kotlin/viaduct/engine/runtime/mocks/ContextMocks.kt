package viaduct.engine.runtime.mocks

import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.SimplePerformantInstrumentation
import graphql.schema.GraphQLSchema
import io.mockk.every
import io.mockk.mockk
import viaduct.engine.api.EngineExecutionContext
import viaduct.engine.api.FragmentLoader
import viaduct.engine.api.ViaductSchema
import viaduct.engine.api.fragment.ViaductExecutableFragmentParser
import viaduct.engine.runtime.CompositeLocalContext
import viaduct.engine.runtime.DispatcherRegistry
import viaduct.engine.runtime.EngineExecutionContextFactory
import viaduct.engine.runtime.EngineExecutionContextImpl
import viaduct.engine.runtime.ViaductFragmentLoader
import viaduct.service.api.spi.FlagManager
import viaduct.service.api.spi.mocks.MockFlagManager

class ContextMocks(
    myFullSchema: GraphQLSchema? = null,
    myDispatcherRegistry: DispatcherRegistry? = null,
    myFragmentLoader: FragmentLoader? = null,
    myResolverInstrumentation: Instrumentation? = null,
    myFlagManager: FlagManager? = null,
    myEngineExecutionContextFactory: EngineExecutionContextFactory? = null,
    private val myEngineExecutionContext: EngineExecutionContext? = null,
    myBaseLocalContext: CompositeLocalContext? = null,
) {
    val fullSchema: GraphQLSchema = myFullSchema ?: mockk<GraphQLSchema>()
    val viaductSchema: ViaductSchema = if (myFullSchema != null) {
        ViaductSchema(myFullSchema)
    } else {
        mockk<ViaductSchema> {
            every { schema } returns fullSchema
        }
    }

    val dispatcherRegistry: DispatcherRegistry = myDispatcherRegistry ?: DispatcherRegistry.Empty
    val fragmentLoader: FragmentLoader = myFragmentLoader ?: ViaductFragmentLoader(ViaductExecutableFragmentParser())
    val resolverInstrumentation: Instrumentation = myResolverInstrumentation ?: SimplePerformantInstrumentation()
    val flagManager: FlagManager = myFlagManager ?: MockFlagManager.Enabled

    val engineExecutionContext: EngineExecutionContext get() =
        when {
            myEngineExecutionContext != null -> myEngineExecutionContext
            else -> engineExecutionContextFactory.create()
        }

    val engineExecutionContextImpl: EngineExecutionContextImpl get() =
        engineExecutionContext as EngineExecutionContextImpl

    val engineExecutionContextFactory =
        myEngineExecutionContextFactory ?: EngineExecutionContextFactory(
            viaductSchema,
            dispatcherRegistry,
            fragmentLoader,
            resolverInstrumentation,
            flagManager,
        )

    val localContext: CompositeLocalContext by lazy {
        myBaseLocalContext?.addOrUpdate(engineExecutionContextImpl) ?: CompositeLocalContext.withContexts(engineExecutionContextImpl)
    }
}
