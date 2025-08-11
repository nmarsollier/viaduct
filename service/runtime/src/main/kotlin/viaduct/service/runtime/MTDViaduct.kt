@file:Suppress("ForbiddenImport")

package viaduct.service.runtime

import graphql.ExecutionResult
import graphql.GraphQL
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import viaduct.engine.api.ViaductSchema
import viaduct.service.api.ExecutionInput
import viaduct.service.api.Viaduct
import viaduct.service.api.airbnb.AirbnbViaduct

/**
 * A mutable implementation of Viaduct interface. It holds two instances of StandardViaduct
 * during schema/code hotswap: the current version and the next version.
 * It uses [strategy pattern](https://en.wikipedia.org/wiki/Strategy_pattern) to delegate
 * queries to one of the StandardViaduct instances.
 */
@Singleton
open class MTDViaduct : Viaduct, AirbnbViaduct {
    @Inject
    internal lateinit var viaductProvider: Provider<Viaduct>

    private val initialState = Pair<StandardViaduct?, StandardViaduct?>(null, null)
    internal var versionsRef: AtomicReference<Pair<StandardViaduct?, StandardViaduct?>> =
        AtomicReference<Pair<StandardViaduct?, StandardViaduct?>>(initialState)

    /**
     * Register the current version of StandardViaduct after Guice initialize the MTDViaduct class.
     * Should only be called once, as for schema swap case we should use beginHotSwap() instead.
     */
    fun init(currViaduct: StandardViaduct): Boolean = versionsRef.compareAndSet(initialState, Pair(currViaduct, null))

    /**
     * Delegate query execution to the picked Viaduct instance
     */
    override fun execute(executionInput: ExecutionInput): ExecutionResult = getCurrentViaduct().execute(executionInput)

    override fun executeAsync(executionInput: ExecutionInput): CompletableFuture<ExecutionResult> = getCurrentViaduct().executeAsync(executionInput)

    override fun getAppliedScopes(schemaId: String): Set<String>? = getCurrentViaduct().getAppliedScopes(schemaId)

    /** Return the graphql-java engine that powers a given schema. */
    override fun getEngine(schemaId: String): GraphQL? = getCurrentViaduct().viaductSchemaRegistry.getEngine(schemaId)

    override fun mkEngineExecutionContext() = getCurrentViaduct().mkEngineExecutionContext()

    /**
     * Get the provider provided Viaduct instance
     */
    private fun getCurrentViaduct(): StandardViaduct {
        if (!isInitialized()) {
            throw IllegalStateException("MTDViaduct hasn't been initialized yet. Please call init() first.")
        }
        return viaductProvider.get() as StandardViaduct
    }

    /**
     * Primitively prioritize returning the next Viaduct for now (not considering cache pre-warming cases)
     * to get the e2e test app working. Will update to more complicated routing logic later.
     * Should be *only* used by the Guice module to pick the current Viaduct in Provider<Viaduct>.
     * Do not use this in other call sites, because it could make the schema bound in Guice different from
     * the schema used in query execution.
     */
    fun routeUseWithCaution(): StandardViaduct {
        val viaduct = versionsRef.get().second ?: versionsRef.get().first
        assert(viaduct != null) {
            "route is called before MTDViaduct is initialized"
        }
        return viaduct!!
    }

    /**
     * Modifies [versions] to kick off a version swap. If there is an ongoing hotswap operation,
     * return false. Let the caller decide whether to drop the hotswap or to retry later.
     *
     * @param nextViaduct The version of Viaduct to be hot swapped to.
     * @return A boolean representing whether the beginning of hotswap succeeds or not.
     */
    fun beginHotSwap(nextViaduct: StandardViaduct): Boolean {
        if (!isInitialized()) {
            throw IllegalStateException("MTDViaduct hasn't been initialized yet. Please call init() first.")
        }
        var canHotSwap = true
        versionsRef.updateAndGet { versions ->
            // A hotswap process is undergoing, do not hotswap again
            if (versions.second != null) {
                canHotSwap = false
                versions
            } else {
                Pair(versions.first, nextViaduct)
            }
        }
        return canHotSwap
    }

    /**
     * Modifies [versions] to end a version swap. In case there is no version swap ongoing,
     * the action would be no-op.
     */
    fun endHotSwap() {
        if (!isInitialized()) {
            throw IllegalStateException("MTDViaduct hasn't been initialized yet. Please call init() first.")
        }
        versionsRef.updateAndGet { versions ->
            if (versions.second == null) {
                versions
            } else {
                Pair(versions.second!!, null)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Will be either private/or somewhere not exposed")
    override fun getSchema(schemaId: String): ViaductSchema? = getCurrentViaduct().getSchema(schemaId)

    private fun isInitialized(): Boolean = versionsRef.get().first != null
}
