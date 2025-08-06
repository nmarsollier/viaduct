package viaduct.tenant.codegen.graphql.bridge

import graphql.language.ArrayValue
import graphql.language.StringValue
import viaduct.graphql.schema.ViaductExtendedSchema
import viaduct.graphql.schema.ViaductSchema

fun ViaductExtendedSchema.Def.isInScope(scope: String): Boolean {
    return when (this) {
        is ViaductExtendedSchema.Enum,
        is ViaductExtendedSchema.Input,
        is ViaductExtendedSchema.Interface,
        is ViaductExtendedSchema.Object,
        is ViaductExtendedSchema.Union -> {
            this.appliedDirectives.includesScope(scope)
        }
        is ViaductExtendedSchema.Field -> {
            this.containingExtension.appliedDirectives.includesScope(scope) && this.type.baseTypeDef.isInScope(scope)
        }
        is ViaductExtendedSchema.EnumValue -> {
            this.containingExtension.appliedDirectives.includesScope(scope)
        }
        else -> true
    }
}

private fun Iterable<ViaductSchema.AppliedDirective>.includesScope(scope: String): Boolean {
    // If a definition is in a non-private scope, it's automatically also in the private version of that scope, e.g.
    // scope(to: ["listing-block"]) is the same as scope(to: ["listing-block", "listing-block:private"]).
    // So if the given scope is :private, expand the check in case the :private scope wasn't explicitly set.
    val scopes = if (scope.endsWith(":private")) {
        setOf(scope, scope.removeSuffix(":private"))
    } else {
        setOf(scope)
    }
    this.filter { it.name == "scope" }.forEach { directive ->
        (directive.arguments["to"] as ArrayValue).values.forEach {
            val value = (it as StringValue).value
            if (value in scopes || value == "*") return true
        }
    }
    return false
}

/** If the extension has a scope directive, return the scopes
 *  listed in that directive.  Otherwise, return null.  Throws
 *  an exception if the directive is not well-formed.
 */
val ViaductExtendedSchema.Extension<*, *>.scopes: List<String>? get() {
    val dirs = this.appliedDirectives.filter { it.name == "scope" }
    if (dirs.size == 0) return null

    // scope is a repeatable directive
    return dirs.flatMap { dir ->
        (dir.arguments["to"] as ArrayValue).values.map { (it as StringValue).value }
    }
}
