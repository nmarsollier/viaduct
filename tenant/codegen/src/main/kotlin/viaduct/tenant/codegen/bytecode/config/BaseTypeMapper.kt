package viaduct.tenant.codegen.bytecode.config

import kotlinx.metadata.KmType
import kotlinx.metadata.KmVariance
import viaduct.codegen.km.KmClassFilesBuilder
import viaduct.codegen.utils.JavaBinaryName
import viaduct.codegen.utils.JavaIdName
import viaduct.codegen.utils.KmName
import viaduct.graphql.schema.ViaductExtendedSchema
import viaduct.tenant.codegen.bytecode.config.cfg.REFLECTION_NAME

/**
 * Interface for handling base type mapping in ViaductSchemaExtensions.
 * This allows AirBnB-specific type mapping logic to be plugged in.
 */
interface BaseTypeMapper {
    fun mapBaseType(
        type: ViaductExtendedSchema.TypeExpr,
        pkg: KmName,
        field: ViaductExtendedSchema.HasDefaultValue?
    ): KmType?

    /**
     * Determines variance for input types based on TypeDef kind.
     * Returns null if default OSS behavior should be used.
     */
    fun getInputVarianceForObject(): KmVariance?

    /**
     * Determines if GlobalID type alias should be used for ID types.
     * Returns true if GlobalID type alias should be used (non-modern builds).
     */
    fun useGlobalIdTypeAlias(): Boolean

    /**
     * Adds external class reference for the given type definition to the KmClassFilesBuilder.
     * This encapsulates the logic for handling different type kinds and build modes.
     */
    fun addSchemaGRTReference(
        def: ViaductExtendedSchema.TypeDef,
        fqn: KmName,
        kmClassFilesBuilder: KmClassFilesBuilder
    )

    /**
     * Provides additional GraphQL type to KmName mappings specific to this mapper.
     */
    fun getAdditionalTypeMapping(): Map<String, KmName>

    /**
     * Provides the GlobalID type appropriate for this mapper.
     * Modern mode uses viaduct.api.globalid.GlobalID (parameterized type).
     */
    fun getGlobalIdType(): viaduct.codegen.utils.JavaBinaryName
}

/**
 * Viaduct implementation of BaseTypeMapper that handles standard cases.
 */
class ViaductBaseTypeMapper : BaseTypeMapper {
    override fun mapBaseType(
        type: ViaductExtendedSchema.TypeExpr,
        pkg: KmName,
        field: ViaductExtendedSchema.HasDefaultValue?
    ): KmType? {
        val baseTypeDef = type.baseTypeDef

        // Handle standard Viaduct cases
        if (baseTypeDef.isBackingDataType) {
            return type.backingDataType()
        } else if (baseTypeDef.isID) {
            return type.idKmType(pkg, this, field)
        }

        return null // Let extension function handle default case
    }

    override fun getInputVarianceForObject(): KmVariance? {
        // Viaduct uses INVARIANT for modern builds (no isModern flag in OSS)
        return KmVariance.INVARIANT
    }

    override fun useGlobalIdTypeAlias(): Boolean {
        // Viaduct (OSS) doesn't use GlobalID type alias
        return false
    }

    override fun addSchemaGRTReference(
        def: ViaductExtendedSchema.TypeDef,
        fqn: KmName,
        kmClassFilesBuilder: KmClassFilesBuilder
    ) {
        val nested = mutableListOf<JavaIdName>()

        // Modern mode: add Reflection nested class for types with reflected types
        if (def.hasReflectedType) {
            nested += JavaIdName(REFLECTION_NAME)
        }

        when (def) {
            is ViaductExtendedSchema.Object -> {
                // Modern mode: objects are treated as classes (not interfaces)
                kmClassFilesBuilder.addExternalClassReference(fqn, nested = nested)
            }
            is ViaductExtendedSchema.Interface, is ViaductExtendedSchema.Union -> {
                kmClassFilesBuilder.addExternalClassReference(fqn, isInterface = true, nested = nested)
            }
            is ViaductExtendedSchema.Input, is ViaductExtendedSchema.Enum -> {
                kmClassFilesBuilder.addExternalClassReference(fqn, nested = nested)
            }
        }
    }

    override fun getAdditionalTypeMapping(): Map<String, KmName> {
        // Modern/OSS mode uses parameterized GlobalID, no additional mapping needed
        return emptyMap()
    }

    override fun getGlobalIdType(): JavaBinaryName {
        return JavaBinaryName("viaduct.api.globalid.GlobalID")
    }
}
