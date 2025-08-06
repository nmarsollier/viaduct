package viaduct.engine.runtime.tenantloading

import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeUtil
import viaduct.engine.api.Coordinate
import viaduct.engine.api.FromArgument
import viaduct.engine.api.RequiredSelectionSet
import viaduct.engine.api.Validated
import viaduct.engine.api.VariablesResolver
import viaduct.engine.api.ViaductSchema
import viaduct.engine.runtime.validation.Validator
import viaduct.graphql.utils.collectVariableUsages
import viaduct.utils.slf4j.logger

/**
 * Validates that all variable references from FromArgumentVariable instances
 * reference valid field arguments in the GraphQL schema, and that the types are compatible.
 * This includes null vs nullable, nested paths, default argument values, and OneOf fields.
 */
class VariableReferencesFromArgumentsExistAndMatchRequiredTypes(
    private val schema: ViaductSchema
) : Validator<RequiredSelectionsValidationCtx> {
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun validate(ctx: RequiredSelectionsValidationCtx) {
        ctx.requiredSelectionSetRegistry
            .getRequiredSelectionSets(ctx.coord.first, ctx.coord.second, true)
            .forEach { selectionSet ->
                validateFromArgumentVariables(ctx, selectionSet)
            }
    }

    private fun validateFromArgumentVariables(
        ctx: RequiredSelectionsValidationCtx,
        rss: RequiredSelectionSet
    ) {
        val fieldCoordinates = FieldCoordinates.coordinates(ctx.coord.first, ctx.coord.second)
        val fieldDef = schema.schema.getFieldDefinition(fieldCoordinates)
            ?: return // Field not found in schema - let other validators handle this

        extractFromArgumentVariables(rss.variablesResolvers).forEach { variable ->
            validateFromArgumentVariable(ctx, variable, fieldDef, rss)
        }
    }

    /**
     * Extracts all [FromArgument] instances from a list of [VariablesResolver]s.
     */
    private fun extractFromArgumentVariables(resolvers: List<VariablesResolver>): List<FromArgument> {
        return resolvers.mapNotNull { resolver ->
            resolver as? FromArgument
                ?: if (resolver is Validated && resolver.delegate is FromArgument) {
                    resolver.delegate as FromArgument
                } else {
                    null
                }
        }
    }

    private fun validateFromArgumentVariable(
        ctx: RequiredSelectionsValidationCtx,
        variable: FromArgument,
        fieldDef: graphql.schema.GraphQLFieldDefinition,
        rss: RequiredSelectionSet
    ) {
        if (variable.path.isEmpty()) {
            throw InvalidArgumentPathException(
                ctx.coord,
                variable.name,
                "Path cannot be empty for FromArgument variable '${variable.name}'."
            )
        }

        // Validate first segment against field arguments
        val firstPathSegment = variable.path.first()
        val argument = fieldDef.arguments.find { it.name == firstPathSegment }
        if (argument == null) {
            throw InvalidArgumentPathException(
                ctx.coord,
                variable.name,
                "Argument '$firstPathSegment' does not exist."
            )
        }

        // Validate path structure and get resolved type
        val initialType = argument.type
        val initialResolvedType = ResolvedType(
            type = initialType,
            nonNullable = GraphQLTypeUtil.isNonNull(initialType)
        )
        val resolvedType = validatePathSegments(variable.path.drop(1), listOf(firstPathSegment), initialResolvedType, ctx, variable, fieldDef)

        // Validate type compatibility for each usage
        val variableUsages = rss.selections.selections.collectVariableUsages(schema.schema, variable.name, ctx.coord)
        variableUsages.forEach { usage ->
            if (!areTypesCompatible(usage.type, resolvedType, usage.hasDefaultValue, usage.isOneOfField)) {
                val errorMessage = if (usage.isOneOfField && !resolvedType.nonNullable) {
                    "OneOf input field '${usage.argumentName}' in field '${usage.fieldName}' requires a non-null value, " +
                        "but variable '${variable.name}' resolves from a nullable path"
                } else {
                    "Type mismatch: variable '${variable.name}' resolves to type '${GraphQLTypeUtil.simplePrint(resolvedType.type)}' " +
                        "but is used in field '${usage.fieldName}' argument '${usage.argumentName}' " +
                        "expecting type '${GraphQLTypeUtil.simplePrint(usage.type)}'"
                }

                throw InvalidArgumentPathException(
                    ctx.coord,
                    variable.name,
                    errorMessage
                )
            }
        }
    }

    /**
     * Validates remaining path segments against a GraphQL type (for nested object traversal).
     * Returns the final type after path traversal.
     */
    private fun validatePathSegments(
        remainingPath: List<String>,
        currentPath: List<String>,
        currentType: ResolvedType,
        ctx: RequiredSelectionsValidationCtx,
        variable: FromArgument,
        fieldDef: graphql.schema.GraphQLFieldDefinition
    ): ResolvedType {
        if (remainingPath.isEmpty()) {
            log.debug("Successfully validated nested path for variable '{}': {}", variable.name, currentPath)
            return currentType
        }

        if (isListType(currentType.type)) {
            throw InvalidArgumentPathException(
                ctx.coord,
                variable.name,
                "Cannot traverse through list type at path segment '${currentPath.joinToString(".")}'. " +
                    "Path traversal through lists is not supported by InputValueReader."
            )
        }

        val fieldName = remainingPath.first()
        val unwrappedType = GraphQLTypeUtil.unwrapAll(currentType.type)

        // Only input object types can have nested fields
        if (unwrappedType !is GraphQLInputObjectType) {
            throw InvalidArgumentPathException(
                ctx.coord,
                variable.name,
                "Cannot traverse to field '$fieldName' from non-object type '${unwrappedType.name}' " +
                    "at path '${currentPath.joinToString(".")}'"
            )
        }

        val field = unwrappedType.getFieldDefinition(fieldName)
        if (field == null) {
            throw InvalidArgumentPathException(
                ctx.coord,
                variable.name,
                "Field '$fieldName' does not exist in input type '${unwrappedType.name}' " +
                    "at path '${currentPath.joinToString(".")}'. "
            )
        }

        val resolvedType = ResolvedType(
            type = field.type,
            nonNullable = currentType.nonNullable && GraphQLTypeUtil.isNonNull(field.type)
        )

        // Continue recursively with the field's type
        return validatePathSegments(
            remainingPath.drop(1),
            currentPath + fieldName,
            resolvedType,
            ctx,
            variable,
            fieldDef
        )
    }

    private fun areTypesCompatible(
        expectedType: GraphQLType,
        resolvedType: ResolvedType,
        hasDefaultValue: Boolean = false,
        isOneOfField: Boolean = false
    ): Boolean {
        // Check for OneOf input usage - OneOf fields effectively require non-null values
        if (isOneOfField && !resolvedType.nonNullable) {
            return false // OneOf field used with nullable path - not compatible
        }

        // Create an effective expected type - if there's a default value, strip NonNull wrapper
        // since the default value can satisfy the non-null requirement
        val effectiveExpectedType = if (hasDefaultValue && expectedType is GraphQLNonNull) {
            expectedType.wrappedType
        } else {
            expectedType
        }

        // Adjust the resolved type based on the nullability of the path
        val adjustedResolvedType = if (resolvedType.nonNullable) {
            // Path is guaranteed non-null, use the field type as-is
            resolvedType.type
        } else {
            // Path could be null, so strip any NonNull wrapper to make it nullable
            GraphQLTypeUtil.unwrapNonNull(resolvedType.type)
        }

        tailrec fun unwrapAndCheckEquality(
            expectedType: GraphQLType,
            resolvedType: GraphQLType
        ): Boolean {
            return when {
                // Handle list coercion: [T]! vs S or [T] vs S -> check if T is compatible with S
                GraphQLTypeUtil.unwrapNonNull(expectedType) is GraphQLList && !isListType(resolvedType) -> {
                    val listType = GraphQLTypeUtil.unwrapNonNull(expectedType) as GraphQLList
                    unwrapAndCheckEquality(listType.wrappedType, resolvedType)
                }
                // Single value can be used as list
                expectedType is GraphQLList && resolvedType !is GraphQLList ->
                    unwrapAndCheckEquality(expectedType.wrappedType, resolvedType)
                // Both are non-null types, unwrap and check equality
                expectedType is GraphQLNonNull && resolvedType is GraphQLNonNull ->
                    unwrapAndCheckEquality(expectedType.wrappedType, resolvedType.wrappedType)
                // Non-null expected but nullable resolved
                expectedType is GraphQLNonNull && resolvedType !is GraphQLNonNull ->
                    false
                // Nullable expected, non-null resolved is OK
                expectedType !is GraphQLNonNull && resolvedType is GraphQLNonNull ->
                    unwrapAndCheckEquality(expectedType, resolvedType.wrappedType)
                // Both are lists, unwrap and check wrapped types
                expectedType is GraphQLList && resolvedType is GraphQLList ->
                    unwrapAndCheckEquality(expectedType.wrappedType, resolvedType.wrappedType)
                else -> expectedType == resolvedType
            }
        }

        return unwrapAndCheckEquality(effectiveExpectedType, adjustedResolvedType)
    }

    /**
     * Checks if a GraphQL type is a list type (including wrapped in NonNull).
     */
    private fun isListType(type: GraphQLType): Boolean {
        return when (type) {
            is GraphQLList -> true
            is GraphQLNonNull -> isListType(type.wrappedType)
            else -> false
        }
    }

    companion object {
        private val log by logger()
    }

    private data class ResolvedType(
        val type: GraphQLType,
        val nonNullable: Boolean
    )
}

/**
 * Exception thrown when a FromArgumentVariable references an invalid argument path.
 */
class InvalidArgumentPathException(
    val coordinate: Coordinate,
    val variableName: String,
    val reason: String
) : Exception() {
    override val message: String
        get() = "Invalid argument path for variable '$variableName' in field '${coordinate.first}.${coordinate.second}': $reason"
}
