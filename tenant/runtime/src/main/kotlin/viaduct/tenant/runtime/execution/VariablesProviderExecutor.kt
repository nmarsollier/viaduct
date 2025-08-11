package viaduct.tenant.runtime.execution

import viaduct.api.VariablesProvider
import viaduct.api.globalid.GlobalIDCodec
import viaduct.api.internal.ReflectionLoader
import viaduct.api.types.Arguments
import viaduct.engine.api.VariablesResolver
import viaduct.tenant.runtime.context.factory.ArgumentsArgs
import viaduct.tenant.runtime.context.factory.Factory
import viaduct.tenant.runtime.internal.InternalContextImpl
import viaduct.tenant.runtime.internal.VariablesProviderInfo

/** Adapts a tenant-defined [VariablesProvider] to a [VariablesResolver] that can be executed by the engine */
class VariablesProviderExecutor(
    val globalIDCodec: GlobalIDCodec,
    val reflectionLoader: ReflectionLoader,
    val variablesProvider: VariablesProviderInfo,
    val argumentsFactory: Factory<ArgumentsArgs, Arguments>,
) : VariablesResolver {
    override val variableNames: Set<String> = variablesProvider.variables

    override suspend fun resolve(ctx: VariablesResolver.ResolveCtx): Map<String, Any?> {
        val provider = variablesProvider.provider.get()
        val internalContext = InternalContextImpl(ctx.engineExecutionContext.fullSchema, globalIDCodec, reflectionLoader)
        val args = ArgumentsArgs(internalContext, ctx.arguments)
        @Suppress("UNCHECKED_CAST")
        return (provider as VariablesProvider<Arguments>).provide(argumentsFactory(args))
    }
}
