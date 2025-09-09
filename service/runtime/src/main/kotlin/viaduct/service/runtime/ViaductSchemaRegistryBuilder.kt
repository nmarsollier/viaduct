package viaduct.service.runtime

import graphql.execution.preparsed.NoOpPreparsedDocumentProvider
import graphql.execution.preparsed.PreparsedDocumentProvider
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import io.github.classgraph.ClassGraph
import java.util.SortedSet
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import viaduct.engine.api.ViaductSchema
import viaduct.engine.api.coroutines.CoroutineInterop
import viaduct.graphql.Scalars
import viaduct.graphql.scopes.ScopedSchemaBuilder
import viaduct.utils.slf4j.logger

class ViaductSchemaLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ViaductSchemaRegistryBuilder {
    private var graphQLSchemaFactory: GraphQLSchemaFactory = GraphQLSchemaFactory.FromRuntimeFiles()
    private var publicSchemaRegistration = mutableListOf<PublicSchemaRegistration>()
    private var asyncSchemaRegistration = ConcurrentHashMap<String, AsyncScopedSchema>()
    var fullSchema: ViaductSchema? = null

    data class AsyncScopedSchema(
        val schemaComputeBlock: () -> ViaductSchema,
        val documentProviderFactory: ((ViaductSchema) -> PreparsedDocumentProvider)?,
        val lazy: Boolean = true
    )

    fun registerSchema(
        schemaId: String,
        scopedSchemaComputeBlock: () -> ViaductSchema,
        documentProviderFactory: ((ViaductSchema) -> PreparsedDocumentProvider)? = null,
        lazy: Boolean = false,
    ): ViaductSchemaRegistryBuilder {
        asyncSchemaRegistration[schemaId] = AsyncScopedSchema(scopedSchemaComputeBlock, documentProviderFactory, lazy)
        return this
    }

    /**
     * Defines the schema with a given instance of the schema
     *
     * @param schema the full schema to register
     */
    fun withFullSchema(schema: ViaductSchema): ViaductSchemaRegistryBuilder {
        graphQLSchemaFactory = GraphQLSchemaFactory.Defined(schema)
        return this
    }

    /**
     * Specifies that the schema should be loaded from files in the project.
     *
     * @param packagePrefix the package prefix to search for the graphqls files, default ""
     * @param resourcesIncluded the regex to match the files to include, default ".*graphqls"
     */
    fun withFullSchemaFromResources(
        packagePrefix: String? = null,
        resourcesIncluded: String? = null,
    ): ViaductSchemaRegistryBuilder {
        graphQLSchemaFactory = GraphQLSchemaFactory.FromRuntimeFiles(
            packagePrefix,
            resourcesIncluded?.toRegex(),
        )
        return this
    }

    /**
     * Specify the schema with a graphqls string.
     *
     * @param sdl the graphqls string to register the schema from
     */
    fun withFullSchemaFromSdl(
        sdl: String,
        customScalars: List<GraphQLScalarType>? = null,
    ): ViaductSchemaRegistryBuilder {
        graphQLSchemaFactory = GraphQLSchemaFactory.FromSdl(
            sdl = sdl,
            customScalars = customScalars,
        )
        return this
    }

    // START OF Register Public Schema methods

    /**
     * This function will be used to register schema scopes from the full schema on the builder.
     *
     * @param scopeIds a set of scopes (the identifiers in @scope directive) to filter the full schema with
     * @param schemaId an identifier used to select the schema against which to run an operation
     *
     */
    fun registerScopedSchema(
        schemaId: String,
        scopeIds: Set<String>
    ): ViaductSchemaRegistryBuilder {
        publicSchemaRegistration.add(PublicSchemaRegistration.ScopedSchema(schemaId, scopeIds))
        return this
    }

    /**
     * This function will be used to register scoped schema using a schema sdl string
     *
     * * @param schemaId an identifier used to select the schema against which to run an operation
     * * @param schemaSdl the schema to be associated with this schemaId - must be a projection of the full schemas
     *
     */
    fun registerSchemaFromSdl(
        schemaId: String,
        schemaSdl: String,
    ): ViaductSchemaRegistryBuilder {
        publicSchemaRegistration.add(
            PublicSchemaRegistration.SchemaFromSdl(
                schemaId,
                schemaSdl
            )
        )

        return this
    }

    /**
     * This function will be used to register the full schema as public, setting the scopeId as the schemaId
     *
     * @param schemaId an identifier used to select the schema against which to run an operation
     *
     */
    fun registerFullSchema(schemaId: String): ViaductSchemaRegistryBuilder {
        publicSchemaRegistration.add(PublicSchemaRegistration.FullSchema(schemaId))

        return this
    }
    // END OF Register Public Schema methods

    fun build(scopedFuture: CoroutineInterop): ViaductSchemaRegistry {
        fullSchema = fullSchema ?: graphQLSchemaFactory.createSchema(scopedFuture)

        val scopedSchemas: ConcurrentHashMap<String, ViaductSchema> = ConcurrentHashMap()
        val schemaDocumentProvider = ConcurrentHashMap<ViaductSchema, PreparsedDocumentProvider>()

        publicSchemaRegistration.forEach {
            it.buildPublicSchema(fullSchema!!, scopedFuture).let { generatedSchema ->
                scopedSchemas.put(generatedSchema.schemaId, generatedSchema.schema)
                schemaDocumentProvider.put(generatedSchema.schema, generatedSchema.schemaDocumentProvider)
            }
        }

        // Validate that there are no duplicate schema IDs in scopedSchemas and asyncGeneratedSchemas
        val duplicateKeys = scopedSchemas.keys.intersect(asyncSchemaRegistration.keys)
        if (duplicateKeys.isNotEmpty()) {
            throw IllegalStateException("Duplicate schema IDs found in scopedSchemas and asyncGeneratedSchemas: $duplicateKeys")
        }

        return ViaductSchemaRegistry(scopedSchemas, asyncSchemaRegistration, schemaDocumentProvider, fullSchema!!)
    }
}

/**
 * Different strategies to register the public schema
 */
private sealed interface PublicSchemaRegistration {
    data class GeneratedSchema(
        val schemaId: String,
        val schema: ViaductSchema,
        val schemaDocumentProvider: PreparsedDocumentProvider
    )

    fun buildPublicSchema(
        fullSchema: ViaductSchema,
        coroutineInterop: CoroutineInterop
    ): GeneratedSchema

    /**
     * Register the schema from a graphqls string
     *
     * @param schemaId an identifier used to select the schema against which to run an operation
     * @param scopeIds a set of scopes (the identifiers in @scope directive) to filter the full schema with
     */
    class ScopedSchema(
        private val schemaId: String,
        private val scopeIds: Set<String>
    ) : PublicSchemaRegistration {
        override fun buildPublicSchema(
            fullSchema: ViaductSchema,
            coroutineInterop: CoroutineInterop
        ): GeneratedSchema {
            val scopes = fullSchema.scopes()
            val scopedSchema = ScopedSchemaBuilder(
                inputSchema = fullSchema.schema,
                additionalVisitorConstructors = emptyList(),
                validScopes = scopes
            ).applyScopes(scopeIds).filtered

            return GeneratedSchema(schemaId, ViaductSchema(scopedSchema), NoOpPreparsedDocumentProvider())
        }
    }

    /**
     * Register the schema from a graphqls string
     *
     * @param schemaId an identifier used to select the schema against which to run an operation
     */
    class FullSchema(
        private val schemaId: String
    ) : PublicSchemaRegistration {
        override fun buildPublicSchema(
            fullSchema: ViaductSchema,
            coroutineInterop: CoroutineInterop
        ): GeneratedSchema {
            return GeneratedSchema(schemaId, fullSchema, NoOpPreparsedDocumentProvider())
        }
    }

    /**
     * Register a scoped schema with the given schemaSdl string and the given schemaId
     *
     * @param schemaId an identifier used to select the schema against which to run an operation
     */
    class SchemaFromSdl(
        private val schemaId: String,
        private val schemaSdl: String
    ) : PublicSchemaRegistration {
        override fun buildPublicSchema(
            fullSchema: ViaductSchema,
            coroutineInterop: CoroutineInterop
        ): GeneratedSchema {
            val scopedSchema = schemaFromSdl(schemaSdl, coroutineInterop)
            return GeneratedSchema(schemaId, scopedSchema, NoOpPreparsedDocumentProvider())
        }
    }
}

/**
 * Different strategies to build the GraphQLSchema
 */
private sealed interface GraphQLSchemaFactory {
    fun createSchema(coroutineInterop: CoroutineInterop): ViaductSchema

    /**
     * Register the schema from a graphqls string
     *
     * @param sdl the graphqls string to register the schema from
     */
    class FromSdl(
        val sdl: String,
        val customScalars: List<GraphQLScalarType>? = null,
    ) : GraphQLSchemaFactory {
        override fun createSchema(coroutineInterop: CoroutineInterop): ViaductSchema {
            return schemaFromSdl(sdl, coroutineInterop, customScalars)
        }
    }

    /**
     * Register the schema from the graphqls files available during runtime in the classpath
     *
     * @param packagePrefix the package prefix to search for the graphqls files
     * @param filesIncluded the regex to match the files to include
     */
    class FromRuntimeFiles(
        val packagePrefix: String? = null,
        val filesIncluded: Regex? = null,
    ) : GraphQLSchemaFactory {
        companion object {
            private val log by logger()
        }

        override fun createSchema(coroutineInterop: CoroutineInterop): ViaductSchema {
            return schemaFromRuntimeSchemaFiles(
                coroutineInterop,
                filesIncluded ?: Regex(".*graphqls"),
            )
        }

        /**
         * This function is used to get the full schema files available during runtime
         */
        @OptIn(ExperimentalTime::class)
        private fun schemaFromRuntimeSchemaFiles(
            coroutineInterop: CoroutineInterop,
            filesIncluded: Regex,
        ): ViaductSchema {
            val resourceContents = mutableMapOf<String, String>()

            val (resources, elapsedTime) = measureTimedValue {
                ClassGraph().scan().use {
                    it.getResourcesMatchingPattern(filesIncluded.toPattern()).map { res ->
                        val origin = res.classpathElementURI?.toString() ?: res.classpathElementURL?.toString() ?: "unknown"
                        val uniqueKey = "$origin!/${res.path}"

                        val content = res.open().use { stream ->
                            stream.reader(Charsets.UTF_8).readText().trim()
                        }
                        if (content.isEmpty()) {
                            log.warn("Empty schema file found: $uniqueKey")
                        }
                        resourceContents[uniqueKey] = content
                        uniqueKey
                    }
                }
            }

            log.debug(
                "Got {} resources for pattern {} in {}",
                resources.size,
                filesIncluded.toString(),
                elapsedTime
            )

            if (resources.isEmpty()) {
                throw ViaductSchemaLoadException(
                    "No GraphQL schema files found matching pattern '$filesIncluded' in package prefix '$packagePrefix'. " +
                        "Please ensure your .graphqls files are available in the classpath."
                )
            }

            val sdl = resourceContents.values.joinToString("\n")
            if (sdl.isBlank()) {
                throw ViaductSchemaLoadException(
                    "All GraphQL schema files are empty. Found files: ${resources.joinToString(", ")}. " +
                        "Please ensure your .graphqls files contain valid GraphQL schema definitions."
                )
            }

            return try {
                schemaFromSdl(
                    sdl,
                    coroutineInterop,
                    listOf(Scalars.BackingData),
                    resourceContents.keys.toList()
                )
            } catch (e: graphql.schema.idl.errors.SchemaProblem) {
                // Let SchemaProblem pass through unchanged - it already contains detailed error info
                throw e
            } catch (e: Exception) {
                throw ViaductSchemaLoadException(
                    "Failed to parse GraphQL schema from files: ${resources.joinToString(", ")}. " +
                        "Original error: ${e.message}",
                    e
                )
            }
        }
    }

    /**
     * Register the schema from a given instance of the schema
     *
     * @param schema the schema to register
     */
    class Defined(private val schema: ViaductSchema) : GraphQLSchemaFactory {
        override fun createSchema(coroutineInterop: CoroutineInterop): ViaductSchema {
            return schema
        }
    }
}

/**
 * This function is used to get the schema from a graphqls string
 */
private fun schemaFromSdl(
    sdl: String,
    coroutineInterop: CoroutineInterop,
    customScalars: List<GraphQLScalarType>? = null,
    sourceFiles: List<String>? = null,
): ViaductSchema {
    if (sdl.trim().isEmpty()) {
        val sourceInfo = if (sourceFiles?.isNotEmpty() == true) {
            " Source files: ${sourceFiles.joinToString(", ")}"
        } else {
            ""
        }
        throw ViaductSchemaLoadException(
            "GraphQL schema SDL is empty or contains only whitespace.$sourceInfo " +
                "Please provide a valid GraphQL schema definition."
        )
    }

    val tdr = try {
        SchemaParser().parse(sdl)
    } catch (e: Exception) {
        val sourceInfo = if (sourceFiles?.isNotEmpty() == true) {
            " Source files: ${sourceFiles.joinToString(", ")}"
        } else {
            ""
        }
        throw ViaductSchemaLoadException(
            "Failed to parse GraphQL schema.$sourceInfo Original error: ${e.message}",
            e
        )
    }

    val definedScalars = customScalars ?: emptyList()
    val actualWiringFactory = ViaductWiringFactory(coroutineInterop)
    val wiring = RuntimeWiring.newRuntimeWiring().wiringFactory(actualWiringFactory).apply {
        definedScalars.forEach { scalar(it) }
    }.build()

    // Let SchemaProblem and other GraphQL validation errors pass through
    return ViaductSchema(SchemaGenerator().makeExecutableSchema(tdr, wiring))
}

/**
 * This function is used to get all the scopes defined in a GraphQLSchema.
 * Scopes are defined with the directive @scope
 */
fun ViaductSchema.scopes(): SortedSet<String> {
    val result = sortedSetOf<String>()

    this.schema.typeMap.values.forEach { type ->
        if (type !is GraphQLDirectiveContainer) {
            return@forEach
        }

        val allScopes = type.getAppliedDirectives("scope")
            .map {
                it.getArgument("to").getValue<List<String>>()!!
            }
            .flatten()
            .filter { it != "*" }
            .toSet()

        result.addAll(allScopes)
    }

    return result
}
