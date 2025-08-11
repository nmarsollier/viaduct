package viaduct.testapps.fixtures

import graphql.schema.GraphQLAppliedDirective
import viaduct.engine.api.CheckerExecutor
import viaduct.engine.api.CheckerExecutorFactory
import viaduct.engine.api.ViaductSchema

/**
 * This class implements a viaduct modern tenant test app with specific behavior for the @policyCheck directive.
 * This should not be available outside oss/testapps.
 */
internal class TestAppCheckerExecutorFactoryImpl(
    private val schema: ViaductSchema,
) : CheckerExecutorFactory {
    private val graphQLSchema = schema.schema

    override fun checkerExecutorForField(
        typeName: String,
        fieldName: String
    ): CheckerExecutor? {
        val graphqlField = graphQLSchema.getObjectType(typeName)?.getFieldDefinition(fieldName)
            ?: throw IllegalStateException("Cannot find field $fieldName in type $typeName")

        if (!graphqlField.hasAppliedDirective("policyCheck")) {
            return null
        }

        return getCheckerExecutor(
            graphqlField.getAppliedDirective("policyCheck")
        )
    }

    override fun checkerExecutorForType(typeName: String): CheckerExecutor? {
        val graphqlType = graphQLSchema.getObjectType(typeName)
            ?: throw IllegalStateException("Cannot find type $typeName")

        if (!graphqlType.hasAppliedDirective("policyCheck")) {
            return null
        }

        return getCheckerExecutor(
            graphqlType.getAppliedDirective("policyCheck")
        )
    }

    private fun getCheckerExecutor(policyCheckDirective: GraphQLAppliedDirective): CheckerExecutor {
        val canSee = policyCheckDirective.getArgument("canAccess").getValue() as? Boolean
        return TestAppCheckerExecutorImpl(canSee)
    }
}
