package viaduct.tenant.codegen.bytecode

import viaduct.graphql.schema.ViaductExtendedSchema
import viaduct.tenant.codegen.bytecode.config.cfg

internal class GRTClassFilesBuilder(
    args: CodeGenArgs,
) : GRTClassFilesBuilderBase(args) {
    protected override fun setup() {
        cfg.importedClasses.forEach { kmClassFilesBuilder.addImportedClass(it) }
    }

    protected override fun isGenerated(def: ViaductExtendedSchema.TypeDef): Boolean = !def.name.startsWith("__")

    override fun addEnum(def: ViaductExtendedSchema.Enum) {
        if (!isGenerated(def)) return
        enumGen(def)
    }

    override fun addUnion(def: ViaductExtendedSchema.Union) {
        if (!isGenerated(def)) return
        unionGenV2(def)
    }

    override fun addInput(def: ViaductExtendedSchema.Input) {
        if (!isGenerated(def)) return
        inputGen(def)
    }

    override fun addInterface(def: ViaductExtendedSchema.Interface) {
        if (!isGenerated(def)) return
        interfaceGen(def)
    }

    override fun addObject(def: ViaductExtendedSchema.Object) {
        if (!isGenerated(def)) return
        objectGenV2(def)

        for (field in def.fields) {
            if (!field.args.none()) {
                fieldArgumentsInputGen(field)
            }
        }
    }
}
