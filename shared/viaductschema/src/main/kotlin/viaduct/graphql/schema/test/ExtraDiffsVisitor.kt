package viaduct.graphql.schema.test

import viaduct.graphql.schema.ViaductExtendedSchema
import viaduct.invariants.InvariantChecker

/** Both [visitDef] and then (sequentially) the more specific visitor are
 *  called for each [ViaductExtendedSchema.Def] in the schema.  In the case of nested
 *  items, calls are made in a stack-like manner, i.e., [visitDef] is called
 *  on the parent, [visitDef] on the child, then
 *  kind-specific [visitXyz] on the child, then kind-specific [visitAbc on
 *  the parent.  Before any of these are called some basic sanity checking is
 *  done, ie, both params will have the same name, class, etc.
 */
interface ExtraDiffsVisitor {
    fun visitDirective(
        expected: ViaductExtendedSchema.Directive,
        actual: ViaductExtendedSchema.Directive,
        c: InvariantChecker
    ) {}

    fun visitArg(
        expected: ViaductExtendedSchema.Arg,
        actual: ViaductExtendedSchema.Arg,
        c: InvariantChecker
    ) {}

    fun visitDef(
        expected: ViaductExtendedSchema.Def,
        actual: ViaductExtendedSchema.Def,
        c: InvariantChecker
    ) {}

    fun visitEnum(
        expected: ViaductExtendedSchema.Enum,
        actual: ViaductExtendedSchema.Enum,
        c: InvariantChecker
    ) {}

    fun visitEnumValue(
        expected: ViaductExtendedSchema.EnumValue,
        actual: ViaductExtendedSchema.EnumValue,
        c: InvariantChecker
    ) {}

    fun visitField(
        expected: ViaductExtendedSchema.Field,
        actual: ViaductExtendedSchema.Field,
        c: InvariantChecker
    ) {}

    fun visitInput(
        expected: ViaductExtendedSchema.Input,
        actual: ViaductExtendedSchema.Input,
        c: InvariantChecker
    ) {}

    fun visitInterface(
        expected: ViaductExtendedSchema.Interface,
        actual: ViaductExtendedSchema.Interface,
        c: InvariantChecker
    ) {}

    fun visitObject(
        expected: ViaductExtendedSchema.Object,
        actual: ViaductExtendedSchema.Object,
        c: InvariantChecker
    ) {}

    fun visitScalar(
        expected: ViaductExtendedSchema.Scalar,
        actual: ViaductExtendedSchema.Scalar,
        c: InvariantChecker
    ) {}

    fun visitUnion(
        expected: ViaductExtendedSchema.Union,
        actual: ViaductExtendedSchema.Union,
        c: InvariantChecker
    ) {}
}
