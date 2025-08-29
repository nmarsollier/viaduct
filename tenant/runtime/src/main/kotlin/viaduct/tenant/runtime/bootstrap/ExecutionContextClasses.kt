package viaduct.tenant.runtime.bootstrap

import java.util.Locale.getDefault
import kotlin.reflect.KClass
import viaduct.api.context.ExecutionContext
import viaduct.api.context.FieldExecutionContext
import viaduct.api.types.Arguments
import viaduct.api.types.Object
import viaduct.api.types.Query
import viaduct.engine.api.ViaductSchema

/**
 * Encapsulates logic needed to find classes needed to create
 * [ExecutionContext]s.
 */
open class ExecutionContextClasses<X : ExecutionContext>(
    targetClass: KClass<X>,
    resolverBaseClass: Class<*>,
) {
    @Suppress("UNCHECKED_CAST")
    val context: KClass<X> =
        resolverBaseClass.declaredClasses.firstOrNull {
            targetClass.java.isAssignableFrom(it)
        }?.kotlin as? KClass<X>
            ?: throw IllegalArgumentException("No nested Context class found in ${resolverBaseClass.name}")
}

class FieldContextClasses<X : FieldExecutionContext<*, *, *, *>>(
    targetClass: KClass<X>,
    tenantResolverClassFinder: TenantResolverClassFinder,
    resolverBaseClass: Class<*>,
    schema: ViaductSchema,
    typeName: String,
    fieldName: String,
) : ExecutionContextClasses<X>(targetClass, resolverBaseClass) {
    @Suppress("UNCHECKED_CAST")
    val query = tenantResolverClassFinder.grtClassForName(schema.schema.queryType.name) as KClass<Query>

    val objectValue: KClass<out Object> =
        tenantResolverClassFinder.grtClassForName(typeName)

    val arguments: KClass<out Arguments> = run {
        val fieldDef =
            schema.schema.getObjectType(typeName)?.let { type ->
                type.getFieldDefinition(fieldName)
                    ?: throw IllegalArgumentException("Type $typeName has no field $fieldName.")
            } ?: throw IllegalArgumentException("No type named $typeName.")

        if (fieldDef.arguments.isEmpty()) {
            Arguments.NoArguments::class
        } else {
            val argsTypeName = "${typeName}_${fieldName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }}_Arguments"
            tenantResolverClassFinder.argumentClassForName(argsTypeName)
        }
    }
}
