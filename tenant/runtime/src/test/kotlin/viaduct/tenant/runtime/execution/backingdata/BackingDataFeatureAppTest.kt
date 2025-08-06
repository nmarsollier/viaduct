@file:Suppress("unused", "ClassName")

package viaduct.tenant.runtime.execution.backingdata

import graphql.schema.GraphQLScalarType
import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.graphql.Scalars
import viaduct.graphql.test.assertEquals
import viaduct.tenant.runtime.execution.backingdata.resolverbases.FooResolvers
import viaduct.tenant.runtime.execution.backingdata.resolverbases.QueryResolvers
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase

class BackingDataFeatureAppTest : FeatureAppTestBase() {
    override var sdl =
        """
    | #START_SCHEMA
    | scalar BackingData
    | directive @resolver on FIELD_DEFINITION
    | directive @visibility(level: String!) on FIELD_DEFINITION
    | directive @backingData(class: String!) on FIELD_DEFINITION
    |
    | type Query {
    |  foo: Foo @resolver
    | }
    |
    | type Foo {
    |   iValue: Int @resolver
    |   sValue: String @resolver
    |   backingDataValue: BackingData
    |     @visibility(level:"private")
    |     @resolver
    |     @backingData(class: "featureapps.example.BackingDataValue")
    | }
    | #END_SCHEMA
        """.trimMargin()

    override var customScalars: List<GraphQLScalarType> = listOf(
        Scalars.BackingData
    )

    // Tenant provided resolvers

    @Resolver
    class Query_FooResolver : QueryResolvers.Foo() {
        override suspend fun resolve(ctx: Context) = Foo.Builder(ctx).build()
    }

    @Resolver
    class Foo_BackingDataValueResolver : FooResolvers.BackingDataValue() {
        // In real life this function would likely reach out to an external
        // service that would return a complex object whose content could be
        // picked apart by resolves for various non-private fields.
        override suspend fun resolve(ctx: Context) = EXPECTED_BACKING_DATA
    }

    @Resolver("backingDataValue")
    class Foo_IValueResolver : FooResolvers.IValue() {
        override suspend fun resolve(ctx: Context) = ctx.objectValue.get<BackingDataValue>("backingDataValue", BackingDataValue::class).i
    }

    @Resolver(
        """
        fragment _ on Foo {
            backingDataValue
         }
        """
    )
    class Foo_SValueResolver : FooResolvers.SValue() {
        override suspend fun resolve(ctx: Context) = ctx.objectValue.get<BackingDataValue>("backingDataValue", BackingDataValue::class).s
    }

    // Test Cases Start here

    @Test
    fun `Backing data is resolved and available to other resolvers`() {
        execute(
            query =
                """
                query backing_data_resolved_available_to_other_resolvers {
                    foo {
                      sValue
                      iValue
                    }
                 }
                """.trimIndent()
        ).assertEquals {
            "data" to {
                "foo" to {
                    "iValue" to EXPECTED_BACKING_DATA.i
                    "sValue" to EXPECTED_BACKING_DATA.s
                }
            }
        }
    }

    companion object {
        val EXPECTED_BACKING_DATA =
            BackingDataValue(10, "Hello, World!")
    }
}

// Feature test app file specific test data
data class BackingDataValue(
    val i: Int,
    val s: String,
)
