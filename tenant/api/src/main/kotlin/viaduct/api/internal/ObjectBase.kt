package viaduct.api.internal

import com.google.common.annotations.VisibleForTesting
import graphql.GraphQLContext
import graphql.schema.GraphQLCompositeType
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeUtil
import graphql.schema.GraphQLUnionType
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import viaduct.api.DynamicOutputValueBuilder
import viaduct.api.ViaductFrameworkException
import viaduct.api.ViaductTenantException
import viaduct.api.ViaductTenantUsageException
import viaduct.api.globalid.GlobalID
import viaduct.api.handleTenantAPIErrors
import viaduct.api.types.NodeObject
import viaduct.api.types.Object
import viaduct.engine.api.EngineObject
import viaduct.engine.api.EngineObjectData
import viaduct.engine.api.NodeReference
import viaduct.engine.api.UnsetSelectionException

/**
 * Base class for object type GRTs
 */
abstract class ObjectBase(
    private val context: InternalContext,
    val engineObject: EngineObject,
) : Object {
    private val fieldCache = ConcurrentHashMap<String, Any>()

    /**
     * Called by the generated GRT getters. For internal framework use only.
     */
    protected suspend fun <T> fetch(
        fieldName: String,
        baseFieldTypeClass: KClass<*>,
        alias: String? = null
    ): T {
        try {
            return get(fieldName, baseFieldTypeClass, alias)
        } catch (ex: Exception) {
            when (ex) {
                is ViaductTenantException -> throw ex
                is EngineObjectDataFetchException -> throw ex.cause!!
                else -> throw ViaductFrameworkException("ObjectBase.fetch failed for ${engineObject.graphQLObjectType.name}.$fieldName. ($ex)", ex)
            }
        }
    }

    /**
     * Fetches the given selection from the EngineObjectData and wraps it into a typed GRT or scalar value.
     * Public function called by extension getter function on the GRT.
     *
     * @param selection the GraphQL response key of a selection (see https://spec.graphql.org/draft/#sec-Field-Alias)
     * @param baseFieldTypeClass the KClass that represents the generated base type of the provided selection
     * @param fieldName the name of the field being selected by [selection]
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> get(
        fieldName: String,
        baseFieldTypeClass: KClass<*>,
        alias: String? = null
    ): T {
        val selection = alias ?: fieldName
        val result = fieldCache.getOrPut(selection) {
            val objectType = engineObject.graphQLObjectType
            val fieldDefinition = objectType.getField(fieldName) ?: throw ViaductFrameworkException(
                "Field $fieldName not found on type ${objectType.name}"
            )
            val fieldValue = try {
                when (engineObject) {
                    is NodeReference -> {
                        // If the EOD is a node reference, only allow access to its ID field
                        if (selection == "id") {
                            engineObject.id
                        } else {
                            throw UnsetSelectionException(
                                selection,
                                objectType,
                                "only id can be accessed on an unresolved Node reference created using Context.nodeFor"
                            )
                        }
                    }
                    is EngineObjectData -> engineObject.fetch(selection)
                    else -> throw ViaductFrameworkException("Unknown EngineObject subclass ${engineObject.javaClass.name}")
                }
            } catch (ex: Exception) {
                when (ex) {
                    is UnsetSelectionException -> throw ViaductTenantUsageException(ex.message, ex)
                    is ViaductTenantException, is ViaductFrameworkException -> throw ex
                    else -> throw EngineObjectDataFetchException("engineObjectData.fetch failed on field $fieldName", ex)
                }
            }

            wrap(fieldDefinition.type, fieldValue, baseFieldTypeClass) ?: NULL_VALUE
        }
        return (if (result == NULL_VALUE) null else result) as T
    }

    private fun wrap(
        type: GraphQLType,
        value: Any?,
        baseFieldTypeClass: KClass<*>
    ): Any? {
        if (value == null) {
            if (GraphQLTypeUtil.isNonNull(type)) {
                throw IllegalArgumentException("Got null value for non-null type ${GraphQLTypeUtil.simplePrint(type)}")
            }
            return null
        }

        return when (val unwrappedType = GraphQLTypeUtil.unwrapNonNull(type)) {
            is GraphQLScalarType -> wrapScalar(unwrappedType, value, baseFieldTypeClass)
            is GraphQLEnumType -> wrapEnum(context, unwrappedType, value)
            is GraphQLList -> wrapList(unwrappedType, value, baseFieldTypeClass)
            is GraphQLCompositeType -> wrapObject(unwrappedType, value)
            else -> throw RuntimeException("Unexpected type ${GraphQLTypeUtil.simplePrint(unwrappedType)}")
        }
    }

    private fun wrapScalar(
        type: GraphQLScalarType,
        value: Any,
        baseFieldTypeClass: KClass<*>
    ): Any {
        // The DateTime scalar type coerces to OffsetDateTime, but we use Instant for GRTs
        if (type.name == "DateTime") {
            return when (value) {
                is Instant -> value
                is String -> OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
                else -> throw RuntimeException("Could not convert $value to Instant.")
            }
        } else if (type.name == "JSON") {
            return value
        } else if (type.name == "BackingData") {
            if (value::class != baseFieldTypeClass) {
                throw IllegalArgumentException(
                    "Expected backing data value to be of type ${baseFieldTypeClass.simpleName}, got ${value::class.simpleName}"
                )
            }
            return value
        } else if (baseFieldTypeClass == GlobalID::class) {
            return context.globalIDCodec.deserialize<NodeObject>(value as String)
        }
        return type.coercing.parseValue(value, GraphQLContext.getDefault(), Locale.getDefault()) ?: throw RuntimeException(
            "Failed to parse value $value for scalar type ${type.name}"
        )
    }

    private fun wrapList(
        type: GraphQLList,
        value: Any,
        baseFieldTypeClass: KClass<*>
    ): List<*> {
        if (value !is List<*>) {
            throw IllegalArgumentException("Got non-list value $value for list type")
        }
        return value.map {
            wrap(GraphQLTypeUtil.unwrapOne(type), it, baseFieldTypeClass)
        }
    }

    private fun wrapObject(
        type: GraphQLCompositeType,
        value: Any
    ): ObjectBase {
        if (value !is EngineObject) {
            throw IllegalArgumentException("Expected value to be an instance of EngineObjectData, got $value")
        }

        if (type is GraphQLObjectType && type.name != value.graphQLObjectType.name) {
            throw IllegalArgumentException(
                "Expected value with GraphQL type ${type.name}, got ${value.graphQLObjectType.name}"
            )
        }

        val klass = context.reflectionLoader.reflectionFor(value.graphQLObjectType.name).kcls
        if (type is GraphQLInterfaceType || type is GraphQLUnionType) {
            val typeKlass = context.reflectionLoader.reflectionFor(type.name).kcls
            if (!klass.isSubclassOf(typeKlass)) {
                throw IllegalArgumentException(
                    "Expected value to be a subtype of ${type.name}, got ${klass.simpleName}"
                )
            }
        }

        if (!klass.isSubclassOf(ObjectBase::class)) {
            throw IllegalArgumentException(
                "Expected baseFieldTypeClass that's a subtype of ObjectBase, got $klass"
            )
        }

        return klass.primaryConstructor!!.call(context, value) as ObjectBase
    }

    /**
     * Usually directly used by tenant developers to build Viaduct object in resolvers by calling
     * `MyType.Builder(context)`, where `MyType` is a generated GRT class that extends ObjectBase class.
     */
    abstract class Builder<T>(
        protected val context: InternalContext,
        private val graphQLObjectType: GraphQLObjectType,
    ) : DynamicOutputValueBuilder<T> {
        private val wrapper = EODBuilderWrapper(graphQLObjectType, context.globalIDCodec)

        protected fun buildEngineObjectData() =
            handleTenantAPIErrors("ObjectBase.Builder.buildEngineObjectData failed") {
                wrapper.getEngineObjectData()
            }

        /**
         * Called by strictly typed static builder-setters in generated GRT
         * to put a field value into the EngineObjectData.
         */
        protected fun putInternal(
            fieldName: String,
            value: Any?
        ) = handleTenantAPIErrors("ObjectBase.Builder.putInternal failed") {
            wrapper.put(fieldName, value)
        }

        /**
         * Dynamic builder function with type check
         */
        final override fun put(
            name: String,
            value: Any?,
        ): Builder<T> {
            typeCheck(name, value)

            wrapper.put(name, value)
            return this
        }

        /**
         * Dynamic builder function with type check and alias support.
         * Only used for unit tests, where we need to associate data with an alias.
         */
        @VisibleForTesting
        internal fun put(
            name: String,
            value: Any?,
            alias: String? = null
        ): Builder<T> {
            typeCheck(name, value)

            wrapper.put(name, value, alias)
            return this
        }

        private fun typeCheck(
            fieldName: String,
            value: Any?
        ) {
            val fieldDefinition = graphQLObjectType.getField(fieldName)
                ?: throw IllegalArgumentException("Field $fieldName not found on type ${graphQLObjectType.name}")
            val fieldContext = DynamicValueBuilderTypeChecker.FieldContext(fieldDefinition, graphQLObjectType)
            DynamicValueBuilderTypeChecker(context).checkType(fieldDefinition.type, value, fieldContext)
        }
    }

    // Internal for testing
    internal class EngineObjectDataFetchException(message: String, cause: Throwable) : RuntimeException(message, cause)

    companion object {
        // Used to represent null in the field cache, since ConcurrentHashMap does not allow null values
        private const val NULL_VALUE = "OBJECTBASE_GRT_NULL"
    }
}
