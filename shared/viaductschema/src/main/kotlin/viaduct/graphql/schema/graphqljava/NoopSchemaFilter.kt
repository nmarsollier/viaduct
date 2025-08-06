package viaduct.graphql.schema.graphqljava

import viaduct.graphql.schema.SchemaFilter
import viaduct.graphql.schema.ViaductExtendedSchema

class NoopSchemaFilter : SchemaFilter {
    override fun includeTypeDef(typeDef: ViaductExtendedSchema.TypeDef) = true

    override fun includeField(field: ViaductExtendedSchema.Field) = true

    override fun includeEnumValue(enumValue: ViaductExtendedSchema.EnumValue) = true

    override fun includeSuper(
        record: ViaductExtendedSchema.HasExtensionsWithSupers<*, *>,
        superInterface: ViaductExtendedSchema.Interface
    ) = true
}
