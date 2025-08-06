package viaduct.tenant.codegen.bytecode.config

import graphql.language.StringValue
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmType
import kotlinx.metadata.KmTypeProjection
import kotlinx.metadata.KmVariance
import kotlinx.metadata.isNullable
import viaduct.codegen.utils.Km
import viaduct.codegen.utils.KmName
import viaduct.graphql.schema.ViaductExtendedSchema
import viaduct.graphql.schema.ViaductSchema
import viaduct.tenant.codegen.bytecode.config.IdOf.Companion.idOf

/**
 * Whether the type is the GraphQL `ID` scalar type
 */
internal val ViaductExtendedSchema.TypeDef.isID: Boolean
    get() = kind == ViaductExtendedSchema.TypeDefKind.SCALAR && name == "ID"

/**
 * Constructs a KmType for a TypeExpr representing an ID scalar. Logic is as follows:
 * Classic -
 *   com.airbnb.viaduct.types.GlobalID (alias for String)
 * Modern -
 *   If this TypeExpr is for the "id" field on an object that implements Node
 *     -> viaduct.api.globalid.GlobalID<Foo>
 *   If this TypeExpr is for the "id" field on an interface that implements Node or Node itself
 *     -> viaduct.api.globalid.GlobalID<out Foo>
 *   If this TypeExpr is for a field or argument with the @idOf directive where type = "Foo"
 *     -> viaduct.api.globalid.GlobalID<Foo>
 *   Else
 *     -> String
 */
internal fun ViaductExtendedSchema.TypeExpr.idKmType(
    pkg: KmName,
    field: ViaductExtendedSchema.HasDefaultValue?
): KmType {
    val stringKmType = KmType().also {
        it.classifier = KmClassifier.Class(Km.STRING.toString())
        it.isNullable = this.baseTypeNullable
    }
    if (!cfg.isModern) {
        return stringKmType.also {
            it.abbreviatedType = KmType().also {
                it.classifier = KmClassifier.TypeAlias(cfg.CLASSIC_GLOBALID.asKmName.toString())
            }
        }
    }

    field ?: return stringKmType

    val containerType = field.containingDef as? ViaductExtendedSchema.TypeDef
    val isNodeIdField = isNodeIdField(field)
    val idOf = field.appliedDirectives.idOf

    // This is not the `id` field of a Node, nor does it have an @idOf directive, so the type is just String
    if (!isNodeIdField && idOf == null) {
        return stringKmType
    }

    return cfg.MODERN_GLOBALID.asKmName.asType().also {
        it.arguments += if (isNodeIdField) {
            require(idOf == null) {
                "@idOf may not be used on the `id` field of a Node implementation"
            }
            val variance = if (containerType!!.kind == ViaductExtendedSchema.TypeDefKind.OBJECT) {
                KmVariance.INVARIANT
            } else {
                KmVariance.OUT
            }
            KmTypeProjection(
                variance,
                pkg.append("/${field.containingDef.name}").asType()
            )
        } else {
            KmTypeProjection(
                KmVariance.OUT,
                pkg.append("/${idOf!!.type}").asType()
            )
        }
        it.isNullable = this.baseTypeNullable
    }
}

/**
 * A more ergonomic representation of the @idOf directive
 */
data class IdOf(val type: String) {
    companion object {
        private val name: String = "idOf"

        private fun parse(dir: ViaductSchema.AppliedDirective): IdOf {
            require(dir.name == name)
            return IdOf((dir.arguments["type"] as StringValue).value)
        }

        val Iterable<ViaductSchema.AppliedDirective>.idOf: IdOf?
            get() = firstNotNullOfOrNull { if (it.name == name) parse(it) else null }
    }
}

/**
 * Returns the concrete type for GlobalIDs if it's a GlobalID, otherwise null
 */
fun ViaductExtendedSchema.HasDefaultValue.globalIDTypeName(): String? {
    val isNodeIdField = isNodeIdField(this)
    val idOf = this.appliedDirectives.idOf

    if (!isNodeIdField && idOf == null) {
        return null
    }

    return if (isNodeIdField) {
        this.containingDef.name
    } else {
        idOf?.type
    }
}

private fun isNodeIdField(field: ViaductExtendedSchema.HasDefaultValue): Boolean {
    val containerType = field.containingDef as? ViaductExtendedSchema.TypeDef
    return field.name == "id" && containerType?.isNode == true
}
