package viaduct.tenant.runtime.context.factory

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters
import viaduct.api.internal.InternalContext
import viaduct.api.types.Object
import viaduct.engine.api.EngineObjectData

class ObjectArgs(
    val internalContext: InternalContext,
    /** A request-scoped EngineObjectData of the object that a resolver is running on */
    val objectValue: EngineObjectData,
)

object ObjectFactory {
    /** create a [Factory] that returns [Object] values based on the provided [objectCls] */
    fun <T : Object> forClass(objectCls: KClass<out T>): Factory<ObjectArgs, T> {
        require(objectCls.hasRequiredCtor) {
            "Class ${objectCls.qualifiedName} does not define the expected constructor"
        }
        return Factory { args ->
            objectCls.primaryConstructor!!.call(args.internalContext, args.objectValue)
        }
    }

    private val KClass<*>.hasRequiredCtor: Boolean get() =
        primaryConstructor?.valueParameters?.let { params ->
            val classifiers = params.mapNotNull { it.type.classifier as? KClass<*> }
            if (classifiers.size != 2) {
                false
            } else {
                classifiers[0].isSubclassOf(InternalContext::class) &&
                    classifiers[1].isSubclassOf(EngineObjectData::class)
            }
        } ?: false
}
