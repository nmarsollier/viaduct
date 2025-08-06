package viaduct.tenant.codegen.kotlingen.bytecode

import viaduct.graphql.schema.ViaductExtendedSchema

/** This class represents the public API to the Kotlin code generator.  Everything
 *  else in this package (or its subpackages) is either internal or public for
 *  testing purposes.
 *
 *  An instance of this class is created by calling one of the factory methods
 *  (e.g., [builderFrom]).  The factory method will construct and initialize
 *  the code generator.  You then call [addAll], which will generate the classfiles
 *  into memory.  You then call [buildClassfiles] to write the classfiles to disk.
 *
 *  For implementors:
 *
 *  If you want to implement a variant family of GRTs, you write a subclass of
 *  this class and provide a factory method for your subclass.  See
 *  [com.airbnb.viaduct.cli.bytecode.kotlingrts.classic.KotlinGRTFilesBuilderImp]
 *  for a sample implementation.
 */
abstract class KotlinGRTFilesBuilder protected constructor(
    protected val args: KotlinCodeGenArgs,
) {
    open val pkg = args.pkgForGeneratedClasses

    private var isLoaded = false

    fun addAll(schema: ViaductExtendedSchema): KotlinGRTFilesBuilder {
        if (isLoaded) throw IllegalStateException("Can't call addAll twice.")
        setup()
        for (def in schema.types.values) {
            when (def) {
                is ViaductExtendedSchema.Enum -> addEnum(def)
                is ViaductExtendedSchema.Input -> addInput(def)
                is ViaductExtendedSchema.Interface -> addInterface(def)
                is ViaductExtendedSchema.Object -> addObject(def)
                is ViaductExtendedSchema.Scalar -> { /* skip */ }
                is ViaductExtendedSchema.Union -> addUnion(def)
                else -> throw IllegalArgumentException("Can't handle $def")
            }
        }
        subclassAddAll(schema)

        isLoaded = true
        return this
    }

    /**
     * An optional hook that allows subclasses to handle the entire schema.
     *
     * Will be called after individual type adders (e.g. [addEnum]) have been called
     * for each type in the schema.
     */
    protected open fun subclassAddAll(schema: ViaductExtendedSchema) {}

    protected abstract fun addEnum(def: ViaductExtendedSchema.Enum)

    protected abstract fun addInput(def: ViaductExtendedSchema.Input)

    protected abstract fun addInterface(def: ViaductExtendedSchema.Interface)

    protected abstract fun addObject(def: ViaductExtendedSchema.Object)

    protected abstract fun addUnion(def: ViaductExtendedSchema.Union)

    /** First thing [addAll] does is call this function to allow subclasses
     *  to further initialize themselves if needed.
     */
    protected open fun setup() { }

    companion object {
        fun builderFrom(args: KotlinCodeGenArgs): KotlinGRTFilesBuilder = KotlinGRTFilesBuilderImpl(args)
    }
}

/** Return true if the codegen algorithm generates a GRT for this
 *  type.  This method is used by [addSchemaGRTReference] to determine
 *  when a placeholder is needed.  `addXyz` methods will be called
 *  whether or not this is true: they need to check internally if they
 *  should generate or not.  (NOTE: the fact that we don't use this
 *  in [addAll] was required to fully implement the logic for
 *  `_Argument` types in [KotlinGRTFilesBuilder.addObject].)
 */
val ViaductExtendedSchema.TypeDef.isGenerated: Boolean
    get() = !this.name.startsWith("__")
