package viaduct.graphql.schema

/** Abstract representation of a GraphQL schema.  The main entry into
 *  a schema is the [types] map from type-names to
 *  [ViaductSchema.TypeDef] instances.
 *
 *  These interfaces and classes are meant to be subclassed into
 *  specific implementations (e.g., [GJSchema] is an implementation
 *  that wraps a graphql-java schema).  To that end, there are no
 *  default implementations for any function that returns a type
 *  intended to be subtyped (e.g., TypeExpr or Object).  As a
 *  convenience, we have provided a few default implementations for
 *  member functions that return scalars like Boolean and String, and
 *  a few static functions for default implementations of member
 *  functions that return other types.
 *
 *  There are two "flavors" of ViaductSchema: [ViaductSchema] itself,
 *  which is a much more abstracted version of GraphQL schemas that is
 *  well suited for runtime use-cases, and [ViaductExtendedSchema],
 *  which exposes more of the "parse tree" of a GraphQL schema; in
 *  particular, it exposes the "extensions" structure of the schema
 *  (i.e., what definitions appear in what extensions) plus source-
 *  location information, among other details.
 *  [ViaductExtendedSchema] is better suited for use in build-tools
 *  which use this more detailed information while processing a
 *  schema.  Instances of [ViaductExtendedSchema] should always be
 *  subtypes of [ViaductSchema].
 *
 *  There is an important invariant maintained by all implementations
 *  of [ViaductSchema], which is that they are "closed."  Put
 *  differently, object-equality is equality in the schema.  More
 *  specifically, for every TypeDef in the value-set of this map, any
 *  other type-def _reachable_ from that type-def is also in the map,
 *  i.e., `schema[typeDef.name] === typeDef` for all typeDefs
 *  reachable from type definitions in the value-set of a schema.  Put
 *  more simply, within a schema, there's exactly one TypeDef object
 *  for each unique type-definition in the GraphQL schema.  And the
 *  same is true for field- and enum-value definitions.
 *
 *  We have a number of use-cases in which we start with an initial
 *  "full" schema and filter it down to a subset.  For example, we do
 *  this with scopes, and we do this with compilation schemas.  The
 *  class [FilteredSchema] encapsulates the logic of building a
 *  filtered schema: developers provide an instance of [SchemaFilter],
 *  and the constructor of [FilteredSchema] will apply this to a base
 *  schema to generate a filtered one.
 *
 *  The [checkBridgeSchemaInvariants] function checks this
 *  closed-property and other invariants on ViaductExtendedSchemas.
 *  This checker confirms a few, but not most, of GraphQL's
 *  schema-validation rules: it's assumed that schema validation
 *  happens before the construction of a Viaduct(Extended)Schema.
 *  (For example, one uses graphql-java to create a validated
 *  GraphQLSchema objects, and then uses the [GJSchema] class to
 *  create a [ValidatedExtendedSchema] from that). However, there's
 *  one validation rule, where [ValidatedExtendedSchema]s is
 *  more relaxed than the GraphQL spec, which is:
 *  [ValidatedExtendedSchema]s allow "empty types."  That is, object,
 *  interface, and input-object types with no fields; enumerations
 *  with no values, and unions with no members.
 *  [checkBridgeSchemaInvariants] takes a flag,
 *  [SchemaInvariantOptions], that toggles between strict and relaxed
 *  checking of empty types.
 * [ViaductExtendedSchema] supports one serialization mechanism and two
 *  deserialization mechanisms.  The serialization mechanism consists
 *  of the extension function [ViaductExtendedSchema.toRegistry] which
 *  converts a [ViaductExtendedSchema] into a graphql-java
 *  `TypeRegistry`.  From there one can use graphql-java's
 *  pretty-printer to serialize to a text file.  For deserialization,
 *  the class `GJSchema` converts a graphql-java GraphQLSchema and
 *  converts that into a [ViaductExtendedSchema], while the class
 *  [GJSchemaRaw] converts a [TypeRegistry] into a
 *  [ViaductExtendedSchema] (in both cases, graphql-java can also be
 *  used to parse GraphQL text files).  Because [GraphQLSchema]
 *  objects are GraphQL-validated, [GJSchema] are as well, while
 *  [GJSchemaRaw] objects are not.  However, [GJSchemaRaw] is much
 *  faster to create (from raw text files) than [GJSchema].  Once
 *  earlier stages in your build process validate that GraphQL SDL
 *  source text, tools are faster for using [GJSchemaRaw] and assuming
 *  that the schema they are reading is valid.
 *
 *  While it's convenient that [ViaductExtendedSchema] allows for
 *  empty types, empty types can cause interoperability issues with
 *  these serde mechanisms, because graphql-java generally does not
 *  support empty types.  To increase interoperability with
 *  graphql-java and other toolsets, the
 *  [ViaductExtendedSchema.toRegistry] function inserts a fake field
 *  named [ViaductExtendedSchema.VIADUCT_IGNORE_SYMBOL] into every object,
 *  interface, and input-object type in the schema. In addition,
 *  it inserts the fake object-type [ViaductExtendedSchema.VIADUCT_IGNORE_SYMBOL]
 *  into every schema, and adds this fake type as a fake member to
 *  every union of the schema.  This ensures that serialized versions
 *  of [ViaductExtendedSchema] will be valid GraphQL schemas.  On the
 *  other side, both [GJSchema] and [GJSchemaRaw] strip out these fake
 *  entries and the fake type.
 *
 *  There are a number of places in these classes where we need to
 *  represent "real values."  For example, the arguments provided to
 *  an applied directive, the default value of an input field, and the
 *  arguments passed to a field in a selection-set: these all need a
 *  representation of real values.  This set of classes is agnostic
 *  about how that representation is done.  It uses [Any?] as the type
 *  of real values, and does not further specify what that means.  As
 *  a result, for example, the invariant checker for
 *  [ViaductExtendedSchema] does not interrogate those values.
 *  Implementations of these classes should document the specific way
 *  they represent real values.
 */
interface ViaductSchema {
    val types: Map<String, TypeDef>
    val directives: Map<String, Directive>

    /**
     *  The [Object] (type definition) associated with the query-root
     *  of this schema.
     *
     *  Even though the GraphQL spec requires that every schema has a
     *  query-root type, the [ViaductSchema] abstraction is sometimes
     *  used at build-time to represent "partial" schemas, i.e.,
     *  collections of SDL definitions that will be combined into a
     *  complete schema but which, but themselves, are not complete
     *  schemas.  In these scenarios, the query-root type def can be
     *  missing.
     */
    val queryTypeDef: Object?

    val mutationTypeDef: Object?
    val subscriptionTypeDef: Object?

    /** The name and arguments of a directive applied to a schema element
     *  (e.g., a type-definition, field-definition, etc.).  Implementations
     *  of this type must implement "value type" semantics, meaning [equals]
     *  and [hashCode] are based on value equality, not on reference equality.
     */
    interface AppliedDirective {
        val name: String
        val arguments: Map<String, Any?>

        companion object {
            /**
             * This function is used to create an Anonymous Object of the AppliedDirective interface
             * @param name The value to be put for the name of the AppliedDirective
             * @param arguments A Map of String, Any to be put as arguments
             *
             * @return an Anonymous Object of the AppliedDirective instantiated with the parameters.
             */
            fun of(
                name: String,
                arguments: Map<String, Any?>
            ) = object : AppliedDirective {
                override val name = name
                override val arguments = arguments

                override fun equals(other: Any?): Boolean {
                    if (other === this) {
                        return true
                    }
                    if (other == null || other !is AppliedDirective) {
                        return false
                    }
                    if (name != other.name) {
                        return false
                    }
                    return arguments == other.arguments
                }

                override fun hashCode(): Int {
                    return name.hashCode() + 31 * arguments.hashCode()
                }

                override fun toString() =
                    "@$name${
                        if (arguments.entries.isNotEmpty()) {
                            "(${
                                arguments.entries.sortedBy { it.key }.joinToString(", ") {
                                    "${it.key}: ${it.value}"
                                }
                            })"
                        } else {
                            ""
                        }
                    }"
            }
        }
    }

    /** Supertype of all type definitions, as well as field
     *  enum-value definitions.
     */
    interface Def {
        val name: String

        fun hasAppliedDirective(name: String): Boolean

        val appliedDirectives: Collection<AppliedDirective>

        /** Think of this as the inheritable version of toString.  In
         *  exception messages and other diagnostic contexts, we want
         *  a fuller description of schema objects, which is provided
         *  by this method.  Implementing classes should use this
         *  method in their toString implementations.
         */
        fun describe(): String
    }

    interface DirectiveArg : Arg {
        override val containingDef: Directive

        override fun describe() = "DirectiveArg<${containingDef.name}.$name:$type>"
    }

    interface Directive : HasArgs {
        override val args: Collection<DirectiveArg>
        val allowedLocations: Set<Location>
        val isRepeatable: Boolean

        enum class Location {
            QUERY,
            MUTATION,
            SUBSCRIPTION,
            FIELD,
            FRAGMENT_DEFINITION,
            FRAGMENT_SPREAD,
            INLINE_FRAGMENT,
            VARIABLE_DEFINITION,
            SCHEMA,
            SCALAR,
            OBJECT,
            FIELD_DEFINITION,
            ARGUMENT_DEFINITION,
            INTERFACE,
            UNION,
            ENUM,
            ENUM_VALUE,
            INPUT_OBJECT,
            INPUT_FIELD_DEFINITION
        }

        override fun describe() = "Directive<$name>[${if (isRepeatable) "repeatable on" else ""} ${allowedLocations.joinToString("| ")}]"
    }

    interface TypeDef : Def {
        /** True for scalar and enumeration types. */
        val isSimple: Boolean

        /** Returns a _nullable_ type-expr for this def. */
        fun asTypeExpr(): TypeExpr

        /** Returns the set of Object types possibly subsumed by this
         *  type definition.  It's the empty set of types other
         *  than Object, Interface, or Union. */
        val possibleObjectTypes: Set<Object>
    }

    interface Scalar : TypeDef {
        override fun describe() = "Scalar<$name>"
    }

    /** Tagging interface for Object, Interface, and Union, i.e.,
     *  anything that can be a supertype of an object-value type. */
    interface CompositeOutput : TypeDef

    interface Union : TypeDef, CompositeOutput {
        override fun describe() = "Union<$name>"
    }

    interface EnumValue : Def {
        val containingDef: Enum

        override fun describe() = "EnumValue<$name>"
    }

    interface Enum : TypeDef {
        val values: Collection<EnumValue>

        fun value(name: String): EnumValue?

        override fun describe() = "Enum<$name>"
    }

    interface HasDefaultValue {
        val containingDef: Def

        val type: TypeExpr

        /** Returns the default value; throws NoSuchElementException if none is explicit in the schema. */
        val defaultValue: Any?

        /** Returns true if there's an explicitly defined default. */
        val hasDefault: Boolean

        /** If there's an explicit default value in the schema, that's returned.
         *  If the type is nullable, then null is returned.
         *  Otherwise, throws NoSuchElementException.
         */
        val effectiveDefaultValue: Any?

        /** Returns true iff [effectiveDefaultValue] would _not_ throw an exception. */
        val hasEffectiveDefault: Boolean
    }

    /**
     *  Argument to fields, operations, and (in extended schemas) directive-defs.
     *  In the case of arguments to operations, the names of those always start with '$'.
     */
    interface Arg : Def, HasDefaultValue

    interface FieldArg : Arg {
        override val containingDef: Field

        override fun describe() = "FieldArg<${containingDef.containingDef.name}.${containingDef.name}.$name:$type>"
    }

    /**
     *  Definitions that have arguments (fields, operations,
     *   and (in extended schemas) directive-defs).
     */
    interface HasArgs : Def {
        val args: Collection<Arg>
    }

    /** Represents fields for all of the interface, object, and input types. */
    interface Field : HasArgs, HasDefaultValue {
        override val containingDef: Record

        /** List of arguments defined in this field.  Empty for
         *  input types.  Returned in the order they appeared
         *  in the schema source text.
         */
        override val args: Collection<FieldArg>

        /** For fields in interfaces and objects, this is true if
         *  this field definition is overriding one from an
         *  implemented interface.  False in all other cases.
         */
        val isOverride: Boolean

        override fun describe() = "Field<$name:$type>"
    }

    /** Supertype for types that have fields, i.e., input, interface
     *  and object types.
     */
    interface Record : TypeDef {
        val fields: Collection<Field>

        fun field(name: String): Field?

        /** Iteratively calls [field] down a chain of [Record]-valued fields.
         *  Throws [IllegalArgumentException] if a non-[Record] or missing
         *  field is found in the path.
         */
        fun field(path: Iterable<String>): Field
    }

    interface Input : Record {
        override fun describe() = "Input<$name>"
    }

    interface Interface : Record, CompositeOutput {
        /** List of interfaces implemented by this interface. */
        val supers: Collection<Interface>

        override fun describe() = "Interface<$name>"
    }

    interface Object : Record, CompositeOutput {
        /** List of interfaces implemented by this interface. */
        val supers: Collection<Interface>

        /** List of unions that contain this object type. */
        val unions: Collection<Union>

        override fun describe() = "Object<$name>"
    }

    interface Selection {
        val subselections: Collection<Selection>
        val directives: Collection<AppliedDirective>

        interface Conditional : Selection {
            val condition: CompositeOutput
        }

        interface Field : Selection {
            val fieldName: String
            val arguments: Map<String, Any?>
        }
    }

    interface TypeExpr {
        val isSimple: Boolean
        val isList: Boolean
        val isNullable: Boolean

        val baseTypeNullable: Boolean
        val baseTypeDef: TypeDef

        /** Strip all list wrappers (if any) but maintains both
         *  the base type and its nullability.
         */
        fun unwrapLists(): TypeExpr

        /** Returns the list-dept of this type expression.
         *  If it's not a list type, then 0 is returned.
         */
        val listDepth: Int

        /** Returns true if the type-expression is nullable at
         *  list-depth [depth] (where 0 is the "outer" list
         *  and [listDepth-1] is the innermost one).  Throws
         *  [IllegalArgumentException] if [depth] is out of
         *  bounds.
         */
        fun nullableAtDepth(depth: Int): Boolean
    }
}
