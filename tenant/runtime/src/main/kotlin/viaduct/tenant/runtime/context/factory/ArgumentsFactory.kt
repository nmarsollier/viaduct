package viaduct.tenant.runtime.context.factory

import graphql.schema.GraphQLInputObjectType
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaConstructor
import viaduct.api.internal.InternalContext
import viaduct.api.types.Arguments

class ArgumentsArgs(
    /** A service-scoped [InternalContext] */
    val internalContext: InternalContext,
    /** A request-scoped map of untyped arguments provided to a resolver */
    val arguments: Map<String, Any?>,
)

object ArgumentsFactory {
    /** a Factory that always returns [Arguments.NoArguments] */
    val NoArguments: Factory<Any, Arguments.NoArguments> =
        Factory.const(Arguments.NoArguments)

    /**
     * If the provided [argumentsCls] has the expected constructor, return a [Factory]
     * that marshals argument values into the provided class.
     * Otherwise, returns null
     */
    fun ifClass(argumentsCls: KClass<out Arguments>): Factory<ArgumentsArgs, Arguments>? =
        if (argumentsCls.hasRequiredCtor) {
            forClass(argumentsCls)
        } else {
            null
        }

    /**
     * Create a Factory that returns instances of [Arguments] generated from the
     * provided [argumentsCls].
     */
    fun forClass(argumentsCls: KClass<out Arguments>): Factory<ArgumentsArgs, Arguments> =
        if (argumentsCls == Arguments.NoArguments::class) {
            NoArguments
        } else {
            require(argumentsCls.hasRequiredCtor) {
                "Class ${argumentsCls.qualifiedName} does not define the expected constructor"
            }
            val ctor = argumentsCls.primaryConstructor!!.javaConstructor!!.apply {
                isAccessible = true
            }

            Factory { args ->
                val graphqlInputObjectType = Arguments.inputType(
                    argumentsCls.simpleName!!,
                    args.internalContext.schema
                )
                ctor.newInstance(args.internalContext, args.arguments, graphqlInputObjectType) as Arguments
            }
        }

    private val KClass<*>.hasRequiredCtor: Boolean get() =
        primaryConstructor?.valueParameters?.let { params ->
            val classifiers = params.mapNotNull { it.type.classifier as? KClass<*> }
            classifiers.size == 3 &&
                classifiers[0].isSubclassOf(InternalContext::class) &&
                classifiers[1].isSubclassOf(Map::class) &&
                classifiers[2].isSubclassOf(GraphQLInputObjectType::class)
        } ?: false
}
