package viaduct.tenant.codegen.bytecode

import viaduct.graphql.schema.ViaductExtendedSchema
import viaduct.tenant.codegen.bytecode.config.cfg

internal fun GRTClassFilesBuilder.enumGen(def: ViaductExtendedSchema.Enum) {
    kmClassFilesBuilder.enumClassBuilder(
        kmName = def.name.kmFQN(pkg),
        def.values.map { it.name }
    ).also {
        it.addSupertype(cfg.ENUM_GRT.asKmName.asType())

        // add Reflection object
        this.reflectedTypeGen(def, it)
    }
}
