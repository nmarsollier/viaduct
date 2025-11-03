package viaduct.tenant.runtime.featuretests.fixtures

import viaduct.api.globalid.GlobalIDCodec
import viaduct.api.internal.ReflectionLoader
import viaduct.engine.api.Coordinate
import viaduct.engine.api.FieldResolverExecutor
import viaduct.engine.api.NodeResolverExecutor
import viaduct.engine.api.TenantAPIBootstrapper
import viaduct.engine.api.TenantModuleBootstrapper
import viaduct.engine.api.ViaductSchema
import viaduct.service.api.spi.TenantAPIBootstrapperBuilder
import viaduct.tenant.runtime.execution.FieldUnbatchedResolverExecutorImpl
import viaduct.tenant.runtime.execution.NodeBatchResolverExecutorImpl
import viaduct.tenant.runtime.execution.NodeUnbatchedResolverExecutorImpl
import viaduct.tenant.runtime.globalid.GlobalIDCodecImpl
import viaduct.tenant.runtime.internal.ReflectionLoaderImpl

/** Intended for testing only - the implementation is naive and not scalable. */
class FeatureTestTenantAPIBootstrapperBuilder(
    val fieldUnbatchedResolverStubs: Map<Coordinate, FieldUnbatchedResolverStub<*>>,
    val nodeUnbatchedResolverStubs: Map<String, NodeUnbatchedResolverStub>,
    val nodeBatchResolverStubs: Map<String, NodeBatchResolverStub>,
) : TenantAPIBootstrapperBuilder {
    override fun create() =
        object : TenantAPIBootstrapper {
            val module: TenantModuleBootstrapper = FeatureTestTenantModuleBootstrapper(
                fieldUnbatchedResolverStubs,
                nodeUnbatchedResolverStubs,
                nodeBatchResolverStubs,
            )

            override suspend fun tenantModuleBootstrappers() = listOf(module)
        }
}

/** Intended for testing only - the implementation is naive and not scalable. */
class FeatureTestTenantModuleBootstrapper(
    val fieldUnbatchedResolverStubs: Map<Coordinate, FieldUnbatchedResolverStub<*>>,
    val nodeUnbatchedResolverStubs: Map<String, NodeUnbatchedResolverStub>,
    val nodeBatchResolverExecutorStubs: Map<String, NodeBatchResolverStub>,
) : TenantModuleBootstrapper {
    private val reflectionLoader: ReflectionLoader = ReflectionLoaderImpl { name -> Class.forName("viaduct.tenant.runtime.featuretests.fixtures.$name").kotlin }
    private val globalIDCodec: GlobalIDCodec = GlobalIDCodecImpl(reflectionLoader)

    override fun fieldResolverExecutors(schema: ViaductSchema): Iterable<Pair<Coordinate, FieldResolverExecutor>> =
        fieldUnbatchedResolverStubs.mapValues { (coord, stub) ->
            val (objectSelectionSet, querySelectionSet) = stub.requiredSelectionSets(coord, schema.schema, reflectionLoader)
            FieldUnbatchedResolverExecutorImpl(
                objectSelectionSet = objectSelectionSet,
                querySelectionSet = querySelectionSet,
                resolver = stub.resolver,
                resolveFn = stub::resolve,
                resolverId = "${coord.first}.${coord.second}",
                globalIDCodec = globalIDCodec,
                reflectionLoader = reflectionLoader,
                resolverContextFactory = stub.resolverFactory,
                resolverName = stub.resolverName ?: "test-field-unbatched-resolver"
            )
        }.toList()

    override fun nodeResolverExecutors(schema: ViaductSchema): Iterable<Pair<String, NodeResolverExecutor>> =
        nodeUnbatchedResolverStubs.mapValues { (typeName, stub) ->
            NodeUnbatchedResolverExecutorImpl(
                resolver = stub.resolver,
                resolveFunction = stub::resolve,
                typeName = typeName,
                globalIDCodec = globalIDCodec,
                reflectionLoader = reflectionLoader,
                factory = stub.resolverFactory,
            )
        }.toList() + nodeBatchResolverExecutorStubs.mapValues { (typeName, stub) ->
            NodeBatchResolverExecutorImpl(
                resolver = stub.resolver,
                batchResolveFunction = stub::batchResolve,
                typeName = typeName,
                globalIDCodec = globalIDCodec,
                reflectionLoader = reflectionLoader,
                factory = stub.resolverFactory,
            )
        }.toList()
}
