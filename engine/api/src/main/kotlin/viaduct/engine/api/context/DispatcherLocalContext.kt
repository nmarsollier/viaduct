package viaduct.engine.api.context

import kotlinx.coroutines.CoroutineDispatcher

/** A local context that stores the dispatcher to be used for the current GraphQL execution. */
data class DispatcherLocalContext(val dispatcher: CoroutineDispatcher)
