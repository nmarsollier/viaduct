package viaduct.tenant.runtime.context.factory

import graphql.schema.GraphQLCompositeType
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLTypeUtil
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import viaduct.api.internal.InternalContext
import viaduct.api.reflect.Type
import viaduct.api.select.SelectionSet
import viaduct.api.types.CompositeOutput
import viaduct.engine.api.RawSelectionSet
import viaduct.tenant.runtime.select.SelectionSetImpl

class SelectionSetArgs(
    /** A service-scoped [InternalContext] */
    val internalContext: InternalContext,
    /**
     * A request-scoped RawSelectionSet describing the selections of a field.
     * Null if the field does not support selections.
     */
    val selections: RawSelectionSet?,
)

object SelectionSetFactory {
    val NoSelections: Factory<Any, SelectionSet<*>> =
        Factory.const(SelectionSet.NoSelections)

    /**
     * Return a [Factory] that will parse [Args.selections]
     * as a selection set over the type of the provided [field].
     *
     * Returns [SelectionSet.NoSelections] if the type of [field] does not support
     * selections or if [Args.selections] is null.
     */
    fun forField(field: GraphQLFieldDefinition): Factory<SelectionSetArgs, SelectionSet<*>> =
        (GraphQLTypeUtil.unwrapAll(field.type) as? GraphQLCompositeType)
            ?.let { type ->
                forTypeName(type.name)
            } ?: NoSelections

    /**
     * Return a [Factory] that will parse [Args.selections]
     * as a selection set over a GraphQL type described by the provided GRT [cls].
     *
     * Returns [SelectionSet.NoSelections] when [Args.selections] is null.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : CompositeOutput> forClass(cls: KClass<T>): Factory<SelectionSetArgs, SelectionSet<T>> = forType(Type.ofClass(cls)) as Factory<SelectionSetArgs, SelectionSet<T>>

    /**
     * Return a [Factory] that will parse [Args.selections]
     * as a selection set over a GraphQL type with the provided [name]
     *
     * Returns [SelectionSet.NoSelections] when [Args.selections] is null.
     */
    fun forTypeName(name: String): Factory<SelectionSetArgs, SelectionSet<*>> =
        Factory { args ->
            args.internalContext.reflectionLoader.reflectionFor(name).let { type ->
                require(type.kcls.isSubclassOf(CompositeOutput::class)) {
                    "type `${type.name}` is not CompositeOutput"
                }
                @Suppress("UNCHECKED_CAST")
                forType(type as Type<CompositeOutput>).mk(args)
            }
        }

    /**
     * Return a [Factory] that will parse [Args.selections]
     * as a selection set over a GraphQL type with the provided [type].
     *
     * Returns [SelectionSet.NoSelections] when [Args.selections] is null.
     */
    fun forType(type: Type<CompositeOutput>): Factory<SelectionSetArgs, SelectionSet<*>> =
        Factory { args ->
            args.selections?.let { selections ->
                require(type.kcls != CompositeOutput.NotComposite::class) {
                    "received a non-null selection set on a type declared as not-composite: ${type.kcls}"
                }
                SelectionSetImpl(type, selections)
            } ?: SelectionSet.NoSelections
        }
}
