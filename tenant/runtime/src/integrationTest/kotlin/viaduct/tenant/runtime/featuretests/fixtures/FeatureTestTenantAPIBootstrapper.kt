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

/** Intended for testing only - the implementation is naive and not scalable. */
class FeatureTestTenantAPIBootstrapperBuilder(
    val fieldUnbatchedResolverStubs: Map<Coordinate, FieldUnbatchedResolverStub<*>>,
    val nodeUnbatchedResolverStubs: Map<String, NodeUnbatchedResolverStub>,
    val nodeBatchResolverStubs: Map<String, NodeBatchResolverStub>,
    val reflectionLoader: ReflectionLoader,
    val globalIDCodec: GlobalIDCodec,
) : TenantAPIBootstrapperBuilder {
    override fun create() =
        object : TenantAPIBootstrapper {
            val module: TenantModuleBootstrapper = FeatureTestTenantModuleBootstrapper(
                fieldUnbatchedResolverStubs,
                nodeUnbatchedResolverStubs,
                nodeBatchResolverStubs,
                reflectionLoader,
                globalIDCodec,
            )

            override suspend fun tenantModuleBootstrappers() = listOf(module)
        }
}

/** Intended for testing only - the implementation is naive and not scalable. */
class FeatureTestTenantModuleBootstrapper(
    val fieldUnbatchedResolverStubs: Map<Coordinate, FieldUnbatchedResolverStub<*>>,
    val nodeUnbatchedResolverStubs: Map<String, NodeUnbatchedResolverStub>,
    val nodeBatchResolverExecutorStubs: Map<String, NodeBatchResolverStub>,
    val reflectionLoader: ReflectionLoader,
    val globalIDCodec: GlobalIDCodec,
) : TenantModuleBootstrapper {
    override fun fieldResolverExecutors(schema: ViaductSchema): Iterable<Pair<Coordinate, FieldResolverExecutor>> =
        fieldUnbatchedResolverStubs.mapNotNull { (coord, stub) ->
            // Skip resolvers for fields that don't exist in the schema (e.g., after schema hot-swap)
            val graphQLType = schema.schema.getType(coord.first) as? graphql.schema.GraphQLObjectType
                ?: return@mapNotNull null
            if (graphQLType.getFieldDefinition(coord.second) == null) {
                return@mapNotNull null
            }

            val (objectSelectionSet, querySelectionSet) = stub.requiredSelectionSets(coord, schema.schema, reflectionLoader)
            val resolverFactory = stub.resolverFactory(schema, reflectionLoader)
            coord to FieldUnbatchedResolverExecutorImpl(
                objectSelectionSet = objectSelectionSet,
                querySelectionSet = querySelectionSet,
                resolver = stub.resolver,
                resolveFn = stub::resolve,
                resolverId = "${coord.first}.${coord.second}",
                globalIDCodec = globalIDCodec,
                reflectionLoader = reflectionLoader,
                resolverContextFactory = resolverFactory,
                resolverName = stub.resolverName ?: "test-field-unbatched-resolver"
            )
        }

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
