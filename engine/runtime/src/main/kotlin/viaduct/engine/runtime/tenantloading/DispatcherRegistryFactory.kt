@file:Suppress("ForbiddenImport")

package viaduct.engine.runtime.tenantloading

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import viaduct.engine.api.CheckerDispatcher
import viaduct.engine.api.CheckerExecutorFactory
import viaduct.engine.api.Coordinate
import viaduct.engine.api.FieldResolverDispatcher
import viaduct.engine.api.NodeResolverDispatcher
import viaduct.engine.api.TenantAPIBootstrapper
import viaduct.engine.api.TenantModuleException
import viaduct.engine.api.ViaductSchema
import viaduct.engine.runtime.CheckerDispatcherImpl
import viaduct.engine.runtime.DispatcherRegistry
import viaduct.engine.runtime.FieldResolverDispatcherImpl
import viaduct.engine.runtime.NodeResolverDispatcherImpl
import viaduct.engine.runtime.validation.Validator
import viaduct.utils.slf4j.logger

class DispatcherRegistryFactory(
    private val tenantAPIBootstrapper: TenantAPIBootstrapper,
    private val validator: Validator<DispatcherRegistry>,
    private val checkerExecutorFactory: CheckerExecutorFactory,
) {
    companion object {
        private val log by logger()
    }

    /** create and return a validated DispatcherRegistry */
    fun create(schema: ViaductSchema): DispatcherRegistry {
        val fieldResolverDispatchers = mutableMapOf<Coordinate, FieldResolverDispatcher>()
        val nodeResolverDispatchers = mutableMapOf<String, NodeResolverDispatcher>()
        val fieldCheckerDispatchers = mutableMapOf<Coordinate, CheckerDispatcher>()
        val typeCheckerDispatchers = mutableMapOf<String, CheckerDispatcher>()

        val tenantModuleBootstrappers = runBlocking(Dispatchers.Default) {
            tenantAPIBootstrapper.tenantModuleBootstrappers()
        }

        var nonContributingModernBootstrappersPresent = false

        // Concatenate resolvers from all bootstrappers into a single list.
        for (tenant in tenantModuleBootstrappers) {
            val (tenantFieldResolverExecutors, tenantNodeResolverExecutors) = try {
                val tenantFieldResolverExecutors = tenant.fieldResolverExecutors(schema)
                val tenantNodeResolverExecutors = tenant.nodeResolverExecutors()
                Pair(tenantFieldResolverExecutors, tenantNodeResolverExecutors)
            } catch (e: TenantModuleException) {
                log.warn("Could not bootstrap $tenant", e)
                continue // still concatenate everything else, just skipping one tenant
            }

            var tenantContributesExecutors = false
            for ((fieldCoord, executor) in tenantFieldResolverExecutors) {
                fieldResolverDispatchers[fieldCoord] = FieldResolverDispatcherImpl(executor)
                // Enable access controls for resolver fields only
                checkerExecutorFactory.checkerExecutorForField(fieldCoord.first, fieldCoord.second)?.let {
                    fieldCheckerDispatchers[fieldCoord] = CheckerDispatcherImpl(it)
                }
                tenantContributesExecutors = true
            }
            for ((typeName, executor) in tenantNodeResolverExecutors) {
                nodeResolverDispatchers[typeName] = NodeResolverDispatcherImpl(executor)
            }
            for ((typeName, _) in nodeResolverDispatchers) {
                // Enable access controls for node resolvers only
                checkerExecutorFactory.checkerExecutorForType(typeName)?.let {
                    typeCheckerDispatchers[typeName] = CheckerDispatcherImpl(it)
                }
                tenantContributesExecutors = true
            }
            if (!tenantContributesExecutors) {
                log.warn("Bootstrapping $tenant (a ${tenant.javaClass.name}) did not contribute any executors")
                if (tenant.javaClass.simpleName == "ViaductTenantModuleBootstrapper") {
                    nonContributingModernBootstrappersPresent = true
                }
            }
        }
        val dispatcherRegistry = DispatcherRegistry(fieldResolverDispatchers.toMap(), nodeResolverDispatchers.toMap(), fieldCheckerDispatchers.toMap(), typeCheckerDispatchers.toMap())
        if (dispatcherRegistry.isEmpty() && nonContributingModernBootstrappersPresent) {
            try {
                throw TenantModuleException("Refusing to create an empty executor registry for $tenantModuleBootstrappers")
            } catch (e: Exception) {
                log.warn("Could not bootstrap", e)
                throw e
            }
        }
        return dispatcherRegistry.also(validator::validate)
    }
}
