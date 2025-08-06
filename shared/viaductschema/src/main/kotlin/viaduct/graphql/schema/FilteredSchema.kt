package viaduct.graphql.schema

import viaduct.invariants.InvariantChecker

private typealias TypeMap = Map<String, FilteredSchema.TypeDef<out ViaductExtendedSchema.TypeDef>>

/** See KDoc for [ViaductSchema] for a background.
 *
 *  The `xyzTypeNameFromBaseSchema` parameters here work a bit differently from
 *  their analogs in `GJSchemaRaw`.  In `GJSchemaRaw`, they are used to set
 *  the root types, and thus if they name a non-existent type, the right behavior
 *  is to fail.  Here, the base schema is assumed to have (or not have) a root type
 *  defs, and the `xyzTypeNameFromBaseSchema` is intended to pass the name of those
 *  types in.  Now, it might be the case that those types get filtered out, so
 *  it's _not_ an error if they name non-existent types.  The code can't actually
 *  know, however, if `xyzTypeNameFromBaseSchema` is actually from the base schema,
 *  so it _does_ check to ensure that it names an object type.
 *
 *  (There's a bigger issue here that FilteredSchema does not take an actual schema
 *  as a constructor argument. The reason is that ViaductSchema itself is not
 *  parameterized by a `TypeDef` param, which we'd need to make the typing of
 *  FilteredSchema to work. Not sure if we want to fix this.)
 */
@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class FilteredSchema<T : ViaductExtendedSchema.TypeDef>(
    filter: SchemaFilter,
    schemaEntries: Iterable<Map.Entry<String, T>>,
    directiveEntries: Iterable<Map.Entry<String, ViaductExtendedSchema.Directive>>,
    schemaInvariantOptions: SchemaInvariantOptions,
    queryTypeNameFromBaseSchema: String?,
    mutationTypeNameFromBaseSchema: String?,
    subscriptionTypeNameFromBaseSchema: String?
) : ViaductExtendedSchema {
    private val defs: MutableMap<String, TypeDef<out T>> = mutableMapOf()
    override val types: Map<String, TypeDef<out T>> = defs
    override val directives = directiveEntries.associate { (k, v) -> k to Directive(v, defs) }

    init {
        schemaEntries
            .filter { (_, value) -> filter.includeTypeDef(value) }
            .forEach { (k, v) ->
                val wrappedValue =
                    when (v) {
                        is ViaductExtendedSchema.Enum -> Enum(v, defs, filter)
                        is ViaductExtendedSchema.Input -> Input(v, defs, filter)
                        is ViaductExtendedSchema.Interface -> Interface(v, defs, filter)
                        is ViaductExtendedSchema.Object -> Object(v, defs, filter)
                        is ViaductExtendedSchema.Union -> Union(v, defs, filter)
                        is ViaductExtendedSchema.Scalar -> Scalar(v, defs)
                        else -> throw IllegalArgumentException("Unexpected type definition $v")
                    }
                defs[k] = wrappedValue
            }

        val violations = InvariantChecker()
        checkBridgeSchemaInvariants(this, violations, schemaInvariantOptions)
        violations.assertEmptyMultiline("FilteredSchema failed the following invariant checks:\n")
    }

    private fun rootDef(nameFromBaseSchema: String?): ViaductExtendedSchema.Object? {
        // As noted earlier, we shouldn't fail if the named type doesn't exist
        val result = nameFromBaseSchema?.let { types[it] }
        if (result != null && result !is ViaductExtendedSchema.Object) {
            throw IllegalArgumentException("$result is not an object type.")
        }
        return result as? ViaductExtendedSchema.Object
    }

    override val queryTypeDef = rootDef(queryTypeNameFromBaseSchema)
    override val mutationTypeDef = rootDef(mutationTypeNameFromBaseSchema)
    override val subscriptionTypeDef = rootDef(subscriptionTypeNameFromBaseSchema)

    override fun toString() = defs.toString()

    sealed interface Def<D : ViaductExtendedSchema.Def> : ViaductExtendedSchema.Def {
        val unfilteredDef: D

        override fun unwrapAll(): ViaductExtendedSchema.Def = this.unfilteredDef.unwrapAll()
    }

    sealed interface TypeDef<T : ViaductExtendedSchema.TypeDef> : Def<T>, ViaductExtendedSchema.TypeDef {
        override fun asTypeExpr(): TypeExpr<*>

        override val possibleObjectTypes: Set<Object<out ViaductExtendedSchema.Object>>
    }

    sealed interface Arg<D : ViaductExtendedSchema.Def, A : ViaductExtendedSchema.Arg> : HasDefaultValue<D, A>, ViaductExtendedSchema.Arg {
        override val unfilteredDef: A
        override val containingDef: Def<D>
    }

    interface HasArgs<D : ViaductExtendedSchema.Def> : Def<D>, ViaductExtendedSchema.HasArgs {
        override val args: List<Arg<D, out ViaductExtendedSchema.Arg>>
    }

    class DirectiveArg<D : ViaductExtendedSchema.Directive, A : ViaductExtendedSchema.DirectiveArg> internal constructor(
        override val unfilteredDef: A,
        override val containingDef: Directive<D>,
        private val defs: TypeMap
    ) : Arg<D, A>, ViaductExtendedSchema.DirectiveArg by unfilteredDef {
        override val type = TypeExpr(unfilteredDef.type, defs)

        override fun toString() = unfilteredDef.toString()
    }

    class Directive<D : ViaductExtendedSchema.Directive> internal constructor(
        override val unfilteredDef: D,
        private val defs: TypeMap
    ) : HasArgs<D>, ViaductExtendedSchema.Directive by unfilteredDef {
        override val args = unfilteredDef.args.map { DirectiveArg(it, this, defs) }
        override val isRepeatable: Boolean = unfilteredDef.isRepeatable

        override fun toString() = unfilteredDef.toString()
    }

    class Scalar<S : ViaductExtendedSchema.Scalar> internal constructor(
        override val unfilteredDef: S,
        private val defs: TypeMap
    ) : TypeDef<S>, ViaductExtendedSchema.Scalar by unfilteredDef {
        override fun asTypeExpr() = TypeExpr(unfilteredDef.asTypeExpr(), defs)

        override fun toString() = unfilteredDef.toString()

        override val possibleObjectTypes = emptySet<Object<out ViaductExtendedSchema.Object>>()
    }

    class EnumValue<E : ViaductExtendedSchema.Enum, V : ViaductExtendedSchema.EnumValue> internal constructor(
        override val unfilteredDef: V,
        override val containingDef: Enum<E>,
        override val containingExtension: ViaductExtendedSchema.Extension<Enum<E>, EnumValue<E, *>>
    ) : Def<V>, ViaductExtendedSchema.EnumValue by unfilteredDef {
        override fun toString() = unfilteredDef.toString()
    }

    class Enum<E : ViaductExtendedSchema.Enum> internal constructor(
        override val unfilteredDef: E,
        private val defs: TypeMap,
        filter: SchemaFilter
    ) : TypeDef<E>, ViaductExtendedSchema.Enum by unfilteredDef {
        override val extensions: List<ViaductExtendedSchema.Extension<Enum<E>, EnumValue<E, *>>> =
            unfilteredDef.extensions.map { unfilteredExt ->
                makeExtension(this, unfilteredDef, unfilteredExt) { ext ->
                    unfilteredExt.members.filter(filter::includeEnumValue).map { EnumValue(it, this, ext) }
                }
            }

        override val values = extensions.flatMap { it.members }

        override fun value(name: String): EnumValue<E, *>? = values.find { name == it.name }

        override fun asTypeExpr() = TypeExpr(unfilteredDef.asTypeExpr(), defs)

        override val possibleObjectTypes = emptySet<Object<out ViaductExtendedSchema.Object>>()

        override fun toString() = unfilteredDef.toString()
    }

    sealed interface CompositeOutput<C : ViaductExtendedSchema.CompositeOutput> : TypeDef<C>, ViaductExtendedSchema.CompositeOutput

    class Union<U : ViaductExtendedSchema.Union> internal constructor(
        override val unfilteredDef: U,
        private val defs: TypeMap,
        filter: SchemaFilter
    ) : CompositeOutput<U>, ViaductExtendedSchema.Union by unfilteredDef {
        override val extensions: List<ViaductExtendedSchema.Extension<Union<U>, Object<*>>> by lazy {
            unfilteredDef.extensions.map { unfilteredExt ->
                makeExtension(this, unfilteredDef, unfilteredExt) { _ ->
                    unfilteredExt.members.filter(filter::includeTypeDef).map { defs[it.name] as Object<*> }
                }
            }
        }

        override fun asTypeExpr() = TypeExpr(unfilteredDef.asTypeExpr(), defs)

        override val possibleObjectTypes by lazy { extensions.flatMap { it.members }.toSet() }

        override fun toString() = unfilteredDef.toString()
    }

    sealed interface HasDefaultValue<P : ViaductExtendedSchema.Def, H : ViaductExtendedSchema.HasDefaultValue> :
        Def<H>, ViaductExtendedSchema.HasDefaultValue {
        override val containingDef: Def<P>
        override val type: TypeExpr<*>
    }

    class FieldArg<R : ViaductExtendedSchema.Record, F : ViaductExtendedSchema.Field, A : ViaductExtendedSchema.FieldArg> internal constructor(
        override val unfilteredDef: A,
        override val containingDef: Field<R, F>,
        private val defs: TypeMap
    ) : Arg<F, A>, ViaductExtendedSchema.FieldArg by unfilteredDef {
        override val type = TypeExpr(unfilteredDef.type, defs)

        override fun toString() = unfilteredDef.toString()
    }

    class Field<R : ViaductExtendedSchema.Record, F : ViaductExtendedSchema.Field> internal constructor(
        override val unfilteredDef: F,
        override val containingDef: Record<R>,
        override val containingExtension: ViaductExtendedSchema.Extension<Record<R>, Field<R, *>>,
        defs: TypeMap
    ) : HasDefaultValue<R, F>, HasArgs<F>, ViaductExtendedSchema.Field by unfilteredDef {
        override val args = unfilteredDef.args.map { FieldArg(it, this, defs) }
        override val type = TypeExpr(unfilteredDef.type, defs)
        override val isOverride by lazy { ViaductExtendedSchema.isOverride(this) }

        override fun toString() = unfilteredDef.toString()
    }

    sealed interface Record<R : ViaductExtendedSchema.Record> : TypeDef<R>, ViaductExtendedSchema.Record {
        override val fields: List<Field<R, out ViaductExtendedSchema.Field>>

        override fun field(name: String) = fields.find { name == it.name }

        override fun field(path: Iterable<String>): Field<R, out ViaductExtendedSchema.Field> = ViaductExtendedSchema.field(this, path)

        override val supers: List<Interface<*>>
        override val unions: List<Union<*>>
    }

    class Interface<I : ViaductExtendedSchema.Interface> internal constructor(
        override val unfilteredDef: I,
        private val defs: TypeMap,
        filter: SchemaFilter
    ) : Record<I>, CompositeOutput<I>, ViaductExtendedSchema.Interface by unfilteredDef {
        override val extensions: List<ViaductExtendedSchema.ExtensionWithSupers<Interface<I>, Field<I, *>>> by lazy {
            val superNames = supers.map { it.name }.toSet()
            unfilteredDef.extensions.map { unfilteredExt ->
                val newSupers =
                    unfilteredExt.supers
                        .filter { superNames.contains(it.name) }
                        .map { defs[it.name] as Interface<*> }
                makeExtension(this, unfilteredDef, unfilteredExt, newSupers) { ext ->
                    unfilteredExt.members.filter {
                        filter.includeField(it) && filter.includeTypeDef(it.type.baseTypeDef)
                    }.map { Field(it, this, ext, defs) }
                }
            }
        }

        override val fields by lazy { extensions.flatMap { it.members } }

        override fun field(name: String) = super<Record>.field(name)

        override fun field(path: Iterable<String>) = super<Record>.field(path)

        override val supers by lazy { unfilteredDef.filterSupers(filter, defs) }
        override val unions = emptyList<Union<*>>()

        override fun asTypeExpr() = TypeExpr(unfilteredDef.asTypeExpr(), defs)

        override val possibleObjectTypes by lazy {
            unfilteredDef.possibleObjectTypes
                .filter { filter.includePossibleSubType(it, unfilteredDef) }
                .map { defs[it.name] as Object<*> }
                .toSet()
        }

        override fun toString() = unfilteredDef.toString()
    }

    class Object<O : ViaductExtendedSchema.Object> internal constructor(
        override val unfilteredDef: O,
        private val defs: TypeMap,
        filter: SchemaFilter
    ) : Record<O>, CompositeOutput<O>, ViaductExtendedSchema.Object by unfilteredDef {
        override val extensions: List<ViaductExtendedSchema.ExtensionWithSupers<Object<O>, Field<O, *>>> by lazy {
            val superNames = supers.map { it.name }.toSet()
            unfilteredDef.extensions.map { unfilteredExt ->
                val newSupers =
                    unfilteredExt.supers
                        .filter { superNames.contains(it.name) }
                        .map { defs[it.name] as Interface<*> }
                makeExtension(this, unfilteredDef, unfilteredExt, newSupers) { ext ->
                    unfilteredExt.members.filter {
                        filter.includeField(it) && filter.includeTypeDef(it.type.baseTypeDef)
                    }.map { Field(it, this, ext, defs) }
                }
            }
        }

        override val fields by lazy { extensions.flatMap { it.members } }

        override fun field(name: String) = super<Record>.field(name)

        override fun field(path: Iterable<String>) = super<Record>.field(path)

        override val supers by lazy { unfilteredDef.filterSupers(filter, defs) }
        override val unions by lazy {
            unfilteredDef.unions
                .filter { filter.includeTypeDef(it) }
                .map { defs[it.name] as Union<*> }
        }

        override fun asTypeExpr() = TypeExpr(unfilteredDef.asTypeExpr(), defs)

        override val possibleObjectTypes = setOf(this)

        override fun toString() = unfilteredDef.toString()
    }

    class Input<I : ViaductExtendedSchema.Input> internal constructor(
        override val unfilteredDef: I,
        private val defs: TypeMap,
        filter: SchemaFilter
    ) : Record<I>, ViaductExtendedSchema.Input by unfilteredDef {
        override val supers = emptyList<Interface<*>>()
        override val unions = emptyList<Union<*>>()
        override val extensions: List<ViaductExtendedSchema.Extension<Input<I>, Field<I, *>>> by lazy {
            unfilteredDef.extensions.map { unfilteredExt ->
                makeExtension(this, unfilteredDef, unfilteredExt) { ext ->
                    unfilteredExt.members.filter {
                        filter.includeField(it) && filter.includeTypeDef(it.type.baseTypeDef)
                    }.map { Field(it, this, ext, defs) }
                }
            }
        }

        override val fields by lazy { extensions.flatMap { it.members } }

        override fun field(name: String) = super<Record>.field(name)

        override fun field(path: Iterable<String>) = super<Record>.field(path)

        override fun asTypeExpr() = TypeExpr(unfilteredDef.asTypeExpr(), defs)

        override val possibleObjectTypes = emptySet<Object<out ViaductExtendedSchema.Object>>()

        override fun toString() = unfilteredDef.toString()
    }

    class TypeExpr<T : ViaductExtendedSchema.TypeExpr> internal constructor(
        private val unfilteredTypeExpr: T,
        private val defs: TypeMap
    ) : ViaductExtendedSchema.TypeExpr() {
        override val baseTypeNullable = unfilteredTypeExpr.baseTypeNullable
        override val baseTypeDef: TypeDef<out ViaductExtendedSchema.TypeDef>
            get() {
                val baseTypeDefName = unfilteredTypeExpr.baseTypeDef.name
                return defs[baseTypeDefName]
                    ?: throw IllegalStateException("$baseTypeDefName not found")
            }
        override val listNullable = unfilteredTypeExpr.listNullable

        override fun unwrapLists() = TypeExpr(unfilteredTypeExpr.unwrapLists(), defs)
    }

    companion object {
        private fun ViaductExtendedSchema.HasExtensionsWithSupers<*, *>.filterSupers(
            filter: SchemaFilter,
            defs: TypeMap
        ) = this.supers
            .filter { filter.includeSuper(this, it) && filter.includeTypeDef(it) }
            .map { defs[it.name] as Interface<*> }

        private fun <D : ViaductExtendedSchema.TypeDef, M : ViaductExtendedSchema.Def> makeExtension(
            def: D,
            unfilteredDef: ViaductExtendedSchema.HasExtensions<*, *>,
            unfilteredExt: ViaductExtendedSchema.Extension<*, *>,
            memberFactory: (ViaductExtendedSchema.Extension<D, M>) -> List<M>
        ) = ViaductExtendedSchema.Extension.of(
            def = def,
            memberFactory = memberFactory,
            isBase = unfilteredExt == unfilteredDef.extensions.first(),
            appliedDirectives = unfilteredExt.appliedDirectives,
            sourceLocation = unfilteredExt.sourceLocation
        )

        private fun <D : ViaductExtendedSchema.TypeDef, M : ViaductExtendedSchema.Def> makeExtension(
            def: D,
            unfilteredDef: ViaductExtendedSchema.HasExtensions<*, *>,
            unfilteredExt: ViaductExtendedSchema.Extension<*, *>,
            supers: List<Interface<*>>,
            memberFactory: (ViaductExtendedSchema.Extension<D, M>) -> List<M>
        ) = ViaductExtendedSchema.ExtensionWithSupers.of(
            def = def,
            memberFactory = memberFactory,
            isBase = unfilteredExt == unfilteredDef.extensions.first(),
            appliedDirectives = unfilteredExt.appliedDirectives,
            sourceLocation = unfilteredExt.sourceLocation,
            supers = supers
        )
    }
}

/** See KDoc for [ViaductSchema] for a background. */
interface SchemaFilter {
    fun includeTypeDef(typeDef: ViaductExtendedSchema.TypeDef): Boolean

    fun includeField(field: ViaductExtendedSchema.Field): Boolean

    fun includeEnumValue(enumValue: ViaductExtendedSchema.EnumValue): Boolean

    fun includeSuper(
        record: ViaductExtendedSchema.HasExtensionsWithSupers<*, *>,
        superInterface: ViaductExtendedSchema.Interface
    ): Boolean

    fun includePossibleSubType(
        possibleSubType: ViaductExtendedSchema.HasExtensionsWithSupers<*, *>,
        targetSuperType: ViaductExtendedSchema.Interface
    ): Boolean =
        when {
            possibleSubType.supers.contains(targetSuperType) -> includeSuper(possibleSubType, targetSuperType)
            else ->
                possibleSubType.supers.any {
                    includeSuper(possibleSubType, it) && includePossibleSubType(it, targetSuperType)
                }
        }
}
