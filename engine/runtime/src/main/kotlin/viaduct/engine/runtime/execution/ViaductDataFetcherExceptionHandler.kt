package viaduct.engine.runtime.execution

import com.airbnb.viaduct.errors.ViaductException
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLNamedType
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import viaduct.api.ViaductFrameworkException
import viaduct.api.ViaductTenantException
import viaduct.api.ViaductTenantResolverException
import viaduct.service.api.spi.ResolverErrorBuilder
import viaduct.service.api.spi.ResolverErrorReporter
import viaduct.service.api.spi.ResolverErrorReporter.Companion.ErrorMetadata
import viaduct.utils.slf4j.logger

class ViaductDataFetcherExceptionHandler(val errorReporter: ResolverErrorReporter, val errorBuilder: ResolverErrorBuilder) : DataFetcherExceptionHandler {
    companion object {
        // A map of error types to expose in the GraphQL error extensions
        // This is used to categorize the errors in the extensions for better error handling,
        // and overrides any error type that is already set in the exception.
        private val EXTENSION_ERROR_TYPES_OVERRIDE: Map<Class<*>, String> = mapOf(TimeoutCancellationException::class.java to "TIMEOUT")
        private val log by logger()
    }

    override fun handleException(handlerParameters: DataFetcherExceptionHandlerParameters): CompletableFuture<DataFetcherExceptionHandlerResult> {
        // Process the exception within this thread and return a completed future with the errors
        // Reporting to Upshot is still offloaded to another coroutine dispatcher and done asynchronously when reportException is invoked
        log.debug(
            "Handling exception for field: {}",
            handlerParameters.dataFetchingEnvironment.fieldDefinition.name,
            handlerParameters.exception
        )
        val errors = processException(handlerParameters)
        log.debug(
            "Processed exception for field: {} with error: {}",
            handlerParameters.dataFetchingEnvironment.fieldDefinition.name,
            errors
        )
        return CompletableFuture.completedFuture(
            DataFetcherExceptionHandlerResult.newResult().errors(errors).build()
        )
    }

    // A helper function that processes the exception and returns a list of GraphQLErrors
    private fun processException(params: DataFetcherExceptionHandlerParameters): List<GraphQLError> {
        val exception = unwrapConcurrencyExceptionIfNecessary(params.exception)
        val unwrappedException = unwrapViaductExceptionIfNecessary(exception)

        val env = params.dataFetchingEnvironment
        val operationName: String? = env.operationDefinition.name
        val metadata = getMetadata(params, operationName, exception)
        val errors = getErrors(unwrappedException, env, metadata)
        val message: String = getErrorMessage(operationName, env, metadata)

        errorReporter.reportError(
            unwrappedException,
            params.fieldDefinition,
            env,
            message,
            metadata
        )

        return errors
    }

    /**
     * Builds a map of additional metadata we want to attach to the exception. This is
     * logged as part of the exception message.
     */
    private fun getMetadata(
        params: DataFetcherExceptionHandlerParameters,
        operationName: String?,
        exception: Throwable,
    ): ErrorMetadata {
        if (params.fieldDefinition == null) return ErrorMetadata.EMPTY
        val isFrameworkError = when (exception) {
            is ViaductFrameworkException -> true
            is ViaductTenantException -> false
            else -> null
        }

        val fieldName = params.fieldDefinition?.name
        val parentType = (params.dataFetchingEnvironment.parentType as? GraphQLNamedType)?.name

        val errorType = EXTENSION_ERROR_TYPES_OVERRIDE[exception::class.java]

        return ErrorMetadata(
            fieldName = fieldName,
            parentType = parentType,
            operationName = operationName,
            isFrameworkError = isFrameworkError,
            resolvers = (exception as? ViaductTenantResolverException)?.let(::resolverCallChain),
            errorType = errorType,
        )
    }

    private fun getErrors(
        exception: Throwable,
        env: DataFetchingEnvironment,
        metadata: ErrorMetadata
    ): List<GraphQLError> {
        val errors = errorBuilder.exceptionToGraphQLError(exception, env, metadata)
        return errors
            ?: when (exception) {
                is FieldFetchingException -> listOf(exception.toGraphQLError())
                is ViaductException -> listOf(exception.toGraphQLError(env, metadata.toMap()))
                else ->
                    listOf(
                        GraphqlErrorBuilder.newError(env)
                            .message(exception.javaClass.name + ": " + exception.message)
                            .path(env.executionStepInfo.path)
                            .extensions(metadata.toMap())
                            .build()
                    )
            }
    }

    private fun getErrorMessage(
        operationName: String?,
        env: DataFetchingEnvironment,
        metadata: ErrorMetadata,
    ): String {
        return "Error fetching %s:%s of type %s.%s: %s".format(
            operationName,
            env.executionStepInfo.path,
            (env.parentType as? GraphQLNamedType)?.name,
            (env.fieldType as? GraphQLNamedType)?.name,
            metadata
        )
    }

    private fun resolverCallChain(exception: ViaductTenantResolverException): List<String> {
        return generateSequence(exception) { it.cause as? ViaductTenantResolverException }
            .map { it.resolver }
            .toList()
    }

    /** Unwrap exceptions that are wrapped in Completion/Cancellation/Execution exceptions.  */
    private fun unwrapConcurrencyExceptionIfNecessary(exception: Throwable): Throwable {
        var cause = exception
        while (cause is CompletionException ||
            cause is CancellationException ||
            cause is ExecutionException ||
            cause is InvocationTargetException
        ) {
            val maybeUnwrapped = cause.cause
            cause = maybeUnwrapped ?: break
        }
        return cause
    }

    /**
     * Unwraps exceptions that the Viaduct framework wraps around exceptions from tenant code
     */
    private fun unwrapViaductExceptionIfNecessary(e: Throwable): Throwable {
        return when (e) {
            is ViaductTenantResolverException -> unwrapViaductExceptionIfNecessary(e.cause)
            is FieldFetchingException -> e.cause?.let(::unwrapViaductExceptionIfNecessary) ?: e
            else -> e
        }
    }
}
