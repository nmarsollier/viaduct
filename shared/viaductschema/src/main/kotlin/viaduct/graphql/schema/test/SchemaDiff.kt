package viaduct.graphql.schema.test

import graphql.language.Node
import java.lang.IllegalArgumentException
import viaduct.graphql.schema.ViaductExtendedSchema
import viaduct.graphql.schema.ViaductSchema
import viaduct.invariants.InvariantChecker

class SchemaDiff(
    private val expected: ViaductExtendedSchema,
    private val actual: ViaductExtendedSchema,
    private val checker: InvariantChecker = InvariantChecker(),
    private val extraDiffs: ExtraDiffsVisitor = object : ExtraDiffsVisitor { },
    private val includeIntrospectiveTypes: Boolean = false
) {
    private var done = false

    fun diff(): InvariantChecker {
        if (!done) {
            // Exclude introspective types from the comparison (not all BridgeSchema impls support them)
            visit(
                expected.types.values.filter { !it.name.startsWith("__") },
                actual.types.values.filter { !it.name.startsWith("__") },
                "TYPE"
            )

            visitDirectives(expected.directives.entries, actual.directives.entries)
            done = true
        }
        return checker
    }

    private fun visit(
        expectedDefs: Iterable<ViaductExtendedSchema.Def>,
        actualDefs: Iterable<ViaductExtendedSchema.Def>,
        kind: String
    ) {
        sameNames(
            expectedDefs,
            actualDefs,
            kind,
            ViaductExtendedSchema.Def::name
        ).forEach { visit(it.first, it.second) }
    }

    private fun <T> sameNames(
        expected: Iterable<T>,
        actual: Iterable<T>,
        kind: String,
        namer: (T) -> String
    ): List<Pair<T, T>> {
        val expectedNames = expected.map { namer.invoke(it) }
        val actualNames = actual.map { namer.invoke(it) }
        checker.containsExactlyElementsIn(expectedNames, actualNames, "SAME_${kind}_NAMES")
        val agreedNames = expectedNames.intersect(actualNames)
        return agreedNames.map { name ->
            Pair(expected.find { namer.invoke(it) == name }!!, actual.find { namer.invoke(it) == name }!!)
        }
    }

    private fun visitAppliedDirective(
        expectedDir: ViaductSchema.AppliedDirective,
        actualDir: ViaductSchema.AppliedDirective
    ) {
        sameNames(
            expectedDir.arguments.entries,
            actualDir.arguments.entries,
            "ARG",
            Map.Entry<String, Any?>::key
        ).forEach {
            checker.withContext(it.first.key) {
                checker.isTrue(areNodesEqual(it.first.value, it.second.value), "ARG_VALUE_AGREES")
            }
        }
    }

    private fun visitDirectives(
        expectedDirectives: Iterable<Map.Entry<String, ViaductExtendedSchema.Directive>>,
        actualDirectives: Iterable<Map.Entry<String, ViaductExtendedSchema.Directive>>
    ) {
        sameNames(
            expectedDirectives.map { it.value },
            actualDirectives.map { it.value },
            "DIRECTIVE",
            ViaductExtendedSchema.Directive::name
        ).forEach {
            checker.withContext(it.first.name) {
                visit(it.first.args, it.second.args, "DIRECTIVE_ARG")
            }
        }
    }

    private fun visit(
        expectedDef: ViaductExtendedSchema.Def,
        actualDef: ViaductExtendedSchema.Def
    ) {
        try {
            checker.pushContext(actualDef.name)
            // Checks common for all [BridgeSchema.Def]s
            if (!hasSameKind(expectedDef, actualDef, "DEF_CLASS")) {
                return
            }
            sameNames(
                expectedDef.appliedDirectives,
                actualDef.appliedDirectives,
                "DIRECTIVE",
                ViaductSchema.AppliedDirective::name
            ).forEach {
                checker.withContext(it.first.name) { visitAppliedDirective(it.first, it.second) }
            }

            // Visit custom diff logic
            extraDiffs.visitDef(expectedDef, actualDef, checker)

            // Checks specific to each [BridgeSchema.Def] subclass
            if (expectedDef is ViaductExtendedSchema.HasDefaultValue) {
                cvt(expectedDef, actualDef) { exp, act ->
                    hasSameKind(exp.containingDef, act.containingDef, "CONTAINING_TYPES_AGREE")
                    checker.isEqualTo(exp.type, act.type, "ARG_TYPE_AGREE")
                    checker.isEqualTo(exp.containingDef.name, act.containingDef.name, "CONTAINING_TYPE_NAMES_AGREE")
                    if (checker.isEqualTo(exp.hasDefault, act.hasDefault, "HAS_DEFAULTS_AGREE") && exp.hasDefault) {
                        checker.isTrue(areNodesEqual(exp.defaultValue, act.defaultValue), "DEFAULT_VALUES_AGREE")
                    }
                    if (checker.isEqualTo(exp.hasEffectiveDefault, act.hasEffectiveDefault, "HAS_DEFAULTS_AGREE") &&
                        exp.hasEffectiveDefault
                    ) {
                        checker.isTrue(
                            areNodesEqual(
                                exp.effectiveDefaultValue,
                                act.effectiveDefaultValue
                            ),
                            "DEFAULT_VALUES_AGREE"
                        )
                    }
                }
            }
            if (expectedDef is ViaductExtendedSchema.TypeDef) {
                cvt(expectedDef, actualDef) { exp, act ->
                    checker.isEqualTo(exp.kind, act.kind, "KIND_AGREES")
                    checker.isEqualTo(exp.isSimple, act.isSimple, "IS_SIMPLE_AGREE")
                    checker.isEqualTo(exp.asTypeExpr(), act.asTypeExpr(), "TYPE_EXPR_AGREE")
                    sameNames(
                        exp.possibleObjectTypes,
                        act.possibleObjectTypes,
                        "POSSIBLE_OBJECT_TYPE",
                        ViaductExtendedSchema.Def::name
                    )
                }
            }
            if (expectedDef is ViaductExtendedSchema.HasExtensions<*, *>) {
                cvt(expectedDef, actualDef) { exp, act ->
                    fun ViaductExtendedSchema.Extension<*, *>.memberKeys() = this.members.map { it.name }.sorted().joinToString("::")
                    sameNames(exp.extensions, act.extensions, "EXTENSION", ViaductExtendedSchema.Extension<*, *>::memberKeys)
                }
            }
            if (expectedDef is ViaductExtendedSchema.HasExtensionsWithSupers<*, *>) {
                cvt(expectedDef, actualDef) { exp, act ->
                    fun ViaductExtendedSchema.Extension<*, *>.supersKeys() = this.members.map { it.name }.sorted().joinToString("::")
                    sameNames(exp.extensions, act.extensions, "EXTENSION", ViaductExtendedSchema.Extension<*, *>::supersKeys)
                }
            }
            if (expectedDef is ViaductExtendedSchema.Record) {
                cvt(expectedDef, actualDef) { exp, act ->
                    sameNames(exp.supers, act.supers, "SUPER", ViaductExtendedSchema.Def::name)
                    sameNames(exp.unions, act.unions, "UNION", ViaductExtendedSchema.Def::name)
                    visit(exp.fields, act.fields, "FIELD")
                }
            }
            when (expectedDef) {
                is ViaductExtendedSchema.Arg ->
                    cvt(expectedDef, actualDef) { exp, act ->
                        hasSameKind(exp.containingDef, act.containingDef, "ARG_DEF_KIND_AGREE")
                        checker.isEqualTo(exp.containingDef.name, act.containingDef.name, "ARG_DEF_NAMES_AGREE")
                        extraDiffs.visitArg(exp, act, checker)
                    }
                is ViaductExtendedSchema.Enum ->
                    cvt(expectedDef, actualDef) { exp, act ->
                        visit(exp.values, act.values, "ENUM_VALUE")
                        extraDiffs.visitEnum(exp, act, checker)
                    }

                is ViaductExtendedSchema.EnumValue ->
                    cvt(expectedDef, actualDef) { exp, act ->
                        checker.isEqualTo(
                            exp.containingDef.name,
                            act.containingDef.name,
                            "ENUM_VALUE_CONTAINERS_AGREE"
                        )
                        sameNames(
                            exp.containingDef.appliedDirectives,
                            act.containingDef.appliedDirectives,
                            "EXTENSION_APPLIED_DIRECTIVE",
                            ViaductSchema.AppliedDirective::name
                        ).forEach {
                            checker.withContext(it.first.name) { visitAppliedDirective(it.first, it.second) }
                        }
                        extraDiffs.visitEnumValue(exp, act, checker)
                    }

                is ViaductExtendedSchema.Field ->
                    cvt(expectedDef, actualDef) { exp, act ->
                        checker.isEqualTo(exp.isOverride, act.isOverride, "OVERRIDE_KIND_AGREE")
                        checker.isEqualTo(exp.hasArgs, act.hasArgs, "FIELD_HAS_ARGS_AGREE")
                        sameNames(
                            exp.containingDef.appliedDirectives,
                            act.containingDef.appliedDirectives,
                            "EXTENSION_APPLIED_DIRECTIVE",
                            ViaductSchema.AppliedDirective::name
                        ).forEach {
                            checker.withContext(it.first.name) { visitAppliedDirective(it.first, it.second) }
                        }

                        visit(exp.args, act.args, "ARG")
                        extraDiffs.visitField(exp, act, checker)
                    }

                is ViaductExtendedSchema.Object ->
                    extraDiffs.visitObject(expectedDef, actualDef as ViaductExtendedSchema.Object, checker)

                is ViaductExtendedSchema.Input ->
                    extraDiffs.visitInput(expectedDef, actualDef as ViaductExtendedSchema.Input, checker)

                is ViaductExtendedSchema.Interface ->
                    extraDiffs.visitInterface(expectedDef, actualDef as ViaductExtendedSchema.Interface, checker)

                is ViaductExtendedSchema.Scalar ->
                    extraDiffs.visitScalar(expectedDef, actualDef as ViaductExtendedSchema.Scalar, checker)

                is ViaductExtendedSchema.Union ->
                    extraDiffs.visitUnion(expectedDef, actualDef as ViaductExtendedSchema.Union, checker)

                else -> throw IllegalStateException("Unknown type: $expectedDef")
            }
        } finally {
            checker.popContext()
        }
    }

    private fun areNodesEqual(
        expectedNode: Any?,
        actualNode: Any?
    ): Boolean {
        if (expectedNode != null && (expectedNode as Node<*>).isEqualTo(actualNode as Node<*>)) return true
        return actualNode == null && expectedNode == null
    }

    private fun hasSameKind(
        expectedDef: ViaductExtendedSchema.Def,
        actualDef: ViaductExtendedSchema.Def,
        msg: String
    ): Boolean {
        return when (actualDef) {
            is ViaductExtendedSchema.Directive -> {
                checker.isInstanceOf<ViaductExtendedSchema.Directive>(expectedDef, "${msg}_AGREE")
            }
            is ViaductExtendedSchema.DirectiveArg -> {
                checker.isInstanceOf<ViaductExtendedSchema.DirectiveArg>(expectedDef, "${msg}_AGREE")
            }
            is ViaductExtendedSchema.FieldArg -> {
                checker.isInstanceOf<ViaductExtendedSchema.FieldArg>(expectedDef, "${msg}_AGREE")
            }
            is ViaductExtendedSchema.Enum -> {
                checker.isInstanceOf<ViaductExtendedSchema.Enum>(expectedDef, "${msg}_AGREE")
            }
            is ViaductExtendedSchema.EnumValue -> {
                checker.isInstanceOf<ViaductExtendedSchema.EnumValue>(expectedDef, "${msg}_AGREE")
            }
            is ViaductExtendedSchema.Field -> {
                checker.isInstanceOf<ViaductExtendedSchema.Field>(expectedDef, "${msg}_AGREE")
            }
            is ViaductExtendedSchema.Input -> {
                checker.isInstanceOf<ViaductExtendedSchema.Input>(expectedDef, "${msg}_AGREE")
            }
            is ViaductExtendedSchema.Interface -> {
                checker.isInstanceOf<ViaductExtendedSchema.Interface>(expectedDef, "${msg}_AGREE")
            }
            is ViaductExtendedSchema.Object -> {
                checker.isInstanceOf<ViaductExtendedSchema.Object>(expectedDef, "${msg}_AGREE")
            }
            is ViaductExtendedSchema.Scalar -> {
                checker.isInstanceOf<ViaductExtendedSchema.Scalar>(expectedDef, "${msg}_AGREE")
            }
            is ViaductExtendedSchema.Union -> {
                checker.isInstanceOf<ViaductExtendedSchema.Union>(expectedDef, "${msg}_AGREE")
            }
            else -> throw IllegalArgumentException("Unexpected class $actualDef")
        }
    }

    companion object {
        private inline fun <reified T, R> cvt(
            exp: T,
            act: Any?,
            body: (T, T) -> R
        ): R {
            return body.invoke(exp, act as T)
        }
    }
}
